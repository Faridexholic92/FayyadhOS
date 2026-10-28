import http from 'node:http';
import { timingSafeEqual } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const ROOT = 'https://api.groq.com/openai/v1';
const DEFAULT_MODEL = 'llama-3.1-8b-instant';
class Failure extends Error { constructor(status, code) { super(code); this.status = status; } }
function authenticated(header, token) {
  const a = Buffer.from(header || ''), b = Buffer.from(`Bearer ${token}`);
  return token.length >= 32 && a.length === b.length && timingSafeEqual(a, b);
}
function factsOnly(body) {
  if (!body || body.action !== 'explain_network' || !body.context || typeof body.context !== 'object' || Array.isArray(body.context)) throw new Failure(400, 'invalid_action');
  const c = body.context;
  const transports = ['No connection','Wi-Fi','LAN / Ethernet','Wi-Fi / LAN','VPN','Other network'];
  if (!transports.includes(c.transport) || typeof c.validated !== 'boolean' || typeof c.vpn !== 'boolean' || !Number.isInteger(c.linkMbps) || c.linkMbps < -1 || c.linkMbps > 100000) throw new Failure(400, 'invalid_context');
  const facts = { transport:c.transport, internetValidatedByAndroid:c.validated, vpnTransportReported:c.vpn, wifiLinkMbps:c.linkMbps > 0 ? c.linkMbps : null };
  if ('estimatedDownloadMbps' in c) {
    if (typeof c.estimatedDownloadMbps !== 'number' || !Number.isFinite(c.estimatedDownloadMbps) || c.estimatedDownloadMbps <= 0 || c.estimatedDownloadMbps > 100000) throw new Failure(400,'invalid_context');
    facts.estimatedDownloadMbps = c.estimatedDownloadMbps;
    facts.measurementMethod = 'Earlier user-initiated download sample, at most 8 MiB / roughly 8 seconds including connection setup; not a full benchmark. May no longer reflect current conditions.';
  }
  facts.upload = 'Not measured';
  return facts;
}
async function readJson(stream, maxBytes) {
  let bytes = 0, chunks = [];
  for await (const chunk of stream) { const b = Buffer.from(chunk); bytes += b.length; if (bytes > maxBytes) throw new Failure(413,'payload_too_large'); chunks.push(b); }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { throw new Failure(400,'invalid_json'); }
}
async function providerJson(response) {
  if (!response.ok) {
    await response.body?.cancel();
    throw new Failure(response.status === 401 || response.status === 403 ? 503 : response.status === 429 ? 429 : 502,
      response.status === 401 || response.status === 403 ? 'provider_auth_failed' : response.status === 429 ? 'provider_rate_limited' : 'provider_failed');
  }
  if (!response.headers.get('content-type')?.includes('application/json')) { await response.body?.cancel(); throw new Failure(502,'invalid_provider_response'); }
  try { return await readJson(response.body, 65536); } catch { throw new Failure(502,'invalid_provider_response'); }
}
export function createJarvisServer({ env = process.env, fetchImpl = fetch, timeoutMs = 12000 } = {}) {
  const key = env.GROQ_API_KEY || '', token = env.JARVIS_DEVICE_TOKEN || '', model = env.GROQ_MODEL || DEFAULT_MODEL;
  let busy = false, windowStart = 0, requests = 0, verifiedAt = 0;
  const server = http.createServer(async (req, res) => {
    res.setHeader('Content-Type','application/json; charset=utf-8');res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');
    const reply = (status, value) => { if (!res.destroyed && !res.writableEnded) { res.writeHead(status); res.end(JSON.stringify(value)); } };
    if (req.url !== '/api/jarvis' || req.method !== 'POST') return reply(404,{error:'not_found'});
    if (!authenticated(req.headers.authorization, token)) return reply(401,{error:'unauthorized'});
    if (!key) return reply(503,{error:'provider_not_configured'});
    if (!req.headers['content-type']?.startsWith('application/json')) return reply(415,{error:'json_required'});
    if (busy) return reply(429,{error:'request_in_progress'});
    const now = Date.now();if (now - windowStart >= 60000) { windowStart = now;requests = 0; }
    if (++requests > 6) return reply(429,{error:'rate_limited'});
    busy = true;
    const controller = new AbortController();let timedOut = false;
    const timer = setTimeout(() => { timedOut = true;controller.abort(); },timeoutMs);
    const disconnect = () => { if (!res.writableEnded) controller.abort(); };
    res.on('close',disconnect);
    try {
      // Bound slow request bodies as well as provider requests.
      const abortBody = () => { if (!req.complete) req.destroy(); };
      controller.signal.addEventListener('abort',abortBody,{once:true});
      const body = await readJson(req,2048);controller.signal.removeEventListener('abort',abortBody);
      const facts = factsOnly(body);
      if (controller.signal.aborted) throw new Error('aborted');
      const headers = { Authorization:`Bearer ${key}`, 'Content-Type':'application/json' };
      // Verify the configurable model against the live supported list; no guessed/deprecated fallback.
      if (!verifiedAt || Date.now()-verifiedAt > 3600000) {
        const available = await providerJson(await fetchImpl(`${ROOT}/models`,{ headers, signal:controller.signal, redirect:'error' }));
        if (!Array.isArray(available.data) || !available.data.some(m => m.id === model && m.active !== false)) throw new Failure(503,'configured_model_unavailable');
        verifiedAt = Date.now();
      }
      const completion = await providerJson(await fetchImpl(`${ROOT}/chat/completions`,{
        method:'POST',headers,signal:controller.signal,redirect:'error',body:JSON.stringify({
          model, temperature:0.2, max_completion_tokens:300,
          messages:[
            {role:'system',content:'You are Jarvis on FayyadhOS Android TV. Explain only the supplied Android network facts in concise Bahasa Melayu (at most 120 words). Distinguish Wi-Fi link rate from estimated Internet download; upload is unmeasured. Do not invent weather, speeds, device specifications or live observations. Android validation is not proof of speed. Do not claim to change settings or launch apps. Give safe button-based troubleshooting advice. No tools, microphone or text entry.'},
            {role:'user',content:`Explain these measured/reported facts: ${JSON.stringify(facts)}`}
          ]
        })
      }));
      const choice = completion.choices?.[0], text = choice?.message?.content;
      if (typeof text !== 'string' || !text.trim() || text.length > 2400 || choice.finish_reason !== 'stop') throw new Failure(502,'invalid_provider_response');
      if (controller.signal.aborted) throw new Error('aborted');
      reply(200,{provider:'groq',model,text:text.trim()});
    } catch (error) {
      reply(timedOut ? 504 : error instanceof Failure ? error.status : 502,{error:timedOut ? 'provider_timeout' : error instanceof Failure ? error.message : 'provider_unavailable'});
    } finally { clearTimeout(timer);res.off('close',disconnect);busy = false; }
  });
  server.requestTimeout = 15000;server.headersTimeout = 10000;
  return server;
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if ((process.env.JARVIS_DEVICE_TOKEN || '').length < 32) throw new Error('Set JARVIS_DEVICE_TOKEN to at least 32 random characters');
  createJarvisServer().listen(Number(process.env.PORT || 8080),'127.0.0.1',() => console.log('Jarvis backend listening on loopback; configure HTTPS reverse proxy'));
}
