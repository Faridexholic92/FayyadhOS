import {test} from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {createJarvisServer} from './server.mjs';
const token='test-only-device-token-0123456789abcdef', key='test-only-provider-key';
const context={transport:'Wi-Fi',validated:true,vpn:false,linkMbps:780,estimatedDownloadMbps:48.4};
const json=(o,status=200)=>new Response(JSON.stringify(o),{status,headers:{'Content-Type':'application/json'}});
function fakeProvider(calls, variant='success') { return async (url,options) => {
  calls.push({url,options});
  if(variant==='invalid-key')return json({error:{message:key}},401);
  if(url.endsWith('/models'))return json({data:[{id:variant==='missing-model'?'not-available':'llama-3.1-8b-instant'}]});
  if(variant==='timeout')return new Promise((resolve,reject)=>options.signal.addEventListener('abort',()=>reject(new Error('abort')),{once:true}));
  if(variant==='failure')throw new Error(key);
  if(variant==='invalid')return json({choices:[{message:{content:''}}]});
  return json({choices:[{message:{content:'TEST FIXTURE — NOT A LIVE AI RESPONSE'},finish_reason:'stop'}]});
};}
async function withServer(options,fn){const s=createJarvisServer(options);s.listen(0,'127.0.0.1');await once(s,'listening');try{await fn(`http://127.0.0.1:${s.address().port}/api/jarvis`);}finally{s.closeAllConnections();await new Promise(r=>s.close(r));}}
const post=(url,body={action:'explain_network',context},auth=token)=>fetch(url,{method:'POST',headers:{'Authorization':`Bearer ${auth}`,'Content-Type':'application/json'},body:JSON.stringify(body)});
for(const [variant,status] of [['success',200],['invalid-key',503],['missing-model',503],['timeout',504],['failure',502],['invalid',502]])test(`HTTP flow: ${variant} (provider mocked)`,async()=>{
 const calls=[];await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl:fakeProvider(calls,variant),timeoutMs:120},async url=>{
  const response=await post(url);assert.equal(response.status,status);const body=await response.text();assert.ok(!body.includes(key));
  if(status===200){assert.ok(body.includes('TEST FIXTURE'));const sent=JSON.parse(calls[1].options.body);assert.equal(sent.model,'llama-3.1-8b-instant');assert.ok(sent.messages[1].content.includes('780'));assert.ok(sent.messages[1].content.includes('48.4'));assert.ok(!sent.messages[1].content.includes('ssid'));}
 });
});
test('Missing key is explicit and never calls provider',async()=>{await withServer({env:{JARVIS_DEVICE_TOKEN:token},fetchImpl:()=>assert.fail()},async url=>assert.equal((await post(url)).status,503));});
test('Unauthorized and invalid actions/context cannot call provider',async()=>{await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl:()=>assert.fail()},async url=>{
 assert.equal((await post(url,undefined,'bad')).status,401);
 assert.equal((await post(url,{action:'launch_youtube',context})).status,400);
 assert.equal((await post(url,{action:'explain_network',context:{...context,transport:'arbitrary prompt'}})).status,400);
 assert.equal((await post(url,{action:'explain_network',context:{...context,estimatedDownloadMbps:'pretend'}})).status,400);
});});
test('Extra text and identifying context are stripped; model list is cached',async()=>{const calls=[];await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl:fakeProvider(calls)},async url=>{
 for(let i=0;i<2;i++)assert.equal((await post(url,{action:'explain_network',context:{...context,ssid:'PRIVATE-SSID',prompt:'INJECTED'}})).status,200);
 assert.equal(calls.filter(c=>c.url.endsWith('/models')).length,1);assert.ok(!calls[1].options.body.includes('PRIVATE-SSID'));assert.ok(!calls[1].options.body.includes('INJECTED'));
});});
test('Duplicate in-flight requests are rejected, timeout recovers',async()=>{const calls=[];await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl:fakeProvider(calls,'timeout'),timeoutMs:180},async url=>{
 const first=post(url);while(calls.length<2)await new Promise(r=>setTimeout(r,5));assert.equal((await post(url)).status,429);assert.equal((await first).status,504);assert.equal((await post(url)).status,504);
});});
test('Payload is bounded and six-per-minute rate limit is enforced',async()=>{const calls=[];await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl:fakeProvider(calls)},async url=>{
 assert.equal((await post(url,{action:'explain_network',context,padding:'x'.repeat(3000)})).status,413);
 for(let i=0;i<5;i++)assert.equal((await post(url)).status,200);
 assert.equal((await post(url)).status,429);
});});
test('Client disconnect aborts provider work and frees the request gate',async()=>{
 let aborted=false,block=true;
 const fetchImpl=async(url,options)=>{
  if(url.endsWith('/models'))return json({data:[{id:'llama-3.1-8b-instant'}]});
  if(!block)return json({choices:[{message:{content:'TEST FIXTURE AFTER CANCEL'},finish_reason:'stop'}]});
  return new Promise((resolve,reject)=>options.signal.addEventListener('abort',()=>{aborted=true;reject(new Error('cancelled'));},{once:true}));
 };
 await withServer({env:{GROQ_API_KEY:key,JARVIS_DEVICE_TOKEN:token},fetchImpl,timeoutMs:1000},async url=>{
  const controller=new AbortController();const request=fetch(url,{method:'POST',headers:{'Authorization':`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({action:'explain_network',context}),signal:controller.signal});
  await new Promise(r=>setTimeout(r,40));controller.abort();await assert.rejects(request);
  for(let i=0;i<40&&!aborted;i++)await new Promise(r=>setTimeout(r,5));assert.ok(aborted);block=false;
  assert.equal((await post(url)).status,200);
 });
});
