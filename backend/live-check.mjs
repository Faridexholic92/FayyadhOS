// Explicit opt-in, real provider call. Never substitutes fixture data.
import {createJarvisServer} from './server.mjs';
import {once} from 'node:events';
if(!process.env.GROQ_API_KEY || (process.env.JARVIS_DEVICE_TOKEN || '').length<32){
  console.error('Live verification NOT RUN: configure GROQ_API_KEY and JARVIS_DEVICE_TOKEN on the server.');process.exit(2);
}
const server=createJarvisServer();server.listen(0,'127.0.0.1');await once(server,'listening');
try{
 const response=await fetch(`http://127.0.0.1:${server.address().port}/api/jarvis`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${process.env.JARVIS_DEVICE_TOKEN}`},body:JSON.stringify({action:'explain_network',context:{transport:'No connection',validated:false,vpn:false,linkMbps:-1}})});
 const body=await response.json();
 if(!response.ok || body.provider!=='groq' || !body.text)throw new Error(`Live request failed: HTTP ${response.status}`);
 // No request headers, API key or identifying device data are printed.
 console.log(JSON.stringify({verified_at:new Date().toISOString(),http_status:response.status,provider:body.provider,model:body.model,text:body.text},null,2));
}catch(error){console.error(error.message);process.exitCode=1;}
finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
