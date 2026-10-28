package com.gathin.tvhome;

import android.content.Context;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native direct HTTPS to Groq only. No developer backend, configurable host or redirect. */
class JarvisAiClient {
    static final String ROOT="https://api.groq.com/openai/v1/";
    static final int MAX_BYTES=128*1024;
    enum Code { NO_KEY, MODEL_REQUIRED, AUTH, ACCESS, RATE_LIMIT, PROVIDER, RESPONSE, TIMEOUT, NETWORK, CANCELLED, STORAGE, CHANGED }
    static final class Failure extends IOException {
        final Code code;
        Failure(Code c){super(message(c));code=c;}
    }
    static String message(Code c){switch(c){
        case NO_KEY:return "No API key configured. Open Jarvis AI Settings.";
        case MODEL_REQUIRED:return "Choose a model in Jarvis AI Settings, then test the connection.";
        case AUTH:return "Groq rejected the API key. Replace or remove it in AI Settings.";
        case ACCESS:return "Groq denied access to this model or request. Check the model and account permissions.";
        case RATE_LIMIT:return "Groq rate limit or quota reached. Wait before retrying and check account usage.";
        case PROVIDER:return "Groq service error. Try again later.";
        case RESPONSE:return "Groq returned an invalid or unsupported response.";
        case TIMEOUT:return "Groq request timed out. Check the connection and try again.";
        case CANCELLED:return "AI request cancelled.";
        case STORAGE:return "Saved key cannot be read securely. Replace or remove it in AI Settings.";
        case CHANGED:return "AI settings changed. Retry using the current key and model.";
        default:return "Network unavailable or connection failed. Check Internet access.";
    }}
    static final class Response {final int status;final String type,body;Response(int s,String t,String b){status=s;type=t;body=b;}}
    interface Transport {Response exchange(URL url,String key,byte[] body)throws Exception;void cancel();}
    private final JarvisKeyStore store;
    private final Transport transport;
    private volatile boolean cancelled;
    JarvisAiClient(Context c){this(new JarvisKeyStore(c),new DirectTransport());}
    JarvisAiClient(JarvisKeyStore s,Transport t){store=s;transport=t;}
    private String key()throws Exception{
        if(cancelled)throw new Failure(Code.CANCELLED);
        String key;try{key=store.read();}catch(JarvisKeyStore.StorageFailure e){throw new Failure(Code.STORAGE);}
        if(key.isEmpty())throw new Failure(Code.NO_KEY);return key;
    }
    private JSONObject request(String path,String key,JSONObject payload)throws Exception{
        if(cancelled)throw new Failure(Code.CANCELLED);
        Response r;
        try{r=transport.exchange(new URL(ROOT+path),key,payload==null?null:payload.toString().getBytes(StandardCharsets.UTF_8));}
        catch(java.net.SocketTimeoutException e){throw new Failure(Code.TIMEOUT);}
        catch(Exception e){if(cancelled)throw new Failure(Code.CANCELLED);if(e instanceof Failure)throw e;throw new Failure(Code.NETWORK);}
        if(cancelled)throw new Failure(Code.CANCELLED);
        if(r.status==401)throw new Failure(Code.AUTH);
        if(r.status==403||r.status==404||r.status==400||r.status==422)throw new Failure(Code.ACCESS);
        if(r.status==429)throw new Failure(Code.RATE_LIMIT);
        if(r.status>=500)throw new Failure(Code.PROVIDER);
        if(r.status!=200||r.type==null||!r.type.toLowerCase(Locale.ROOT).contains("application/json")||r.body==null||r.body.length()>MAX_BYTES)throw new Failure(Code.RESPONSE);
        try{return new JSONObject(r.body);}catch(JSONException e){throw new Failure(Code.RESPONSE);}
    }
    List<String> models()throws Exception{
        long revision=store.revision();JSONObject response=request("models",key(),null);
        JSONArray data=response.optJSONArray("data");if(data==null)throw new Failure(Code.RESPONSE);
        TreeSet<String> result=new TreeSet<>();
        for(int i=0;i<data.length();i++){JSONObject row=data.optJSONObject(i);if(row==null||!row.optBoolean("active",true))continue;
            String id=row.optString("id","");if(!id.isEmpty()&&id.length()<=200&&!id.matches(".*[\\p{Cntrl}].*"))result.add(id);}
        if(result.isEmpty())throw new Failure(Code.RESPONSE);
        if(store.revision()!=revision)throw new Failure(Code.CHANGED);return new ArrayList<>(result);
    }
    private String completion(String key,String model,JSONArray messages,int limit)throws Exception{
        JSONObject body=new JSONObject().put("model",model).put("messages",messages).put("stream",false).put("max_completion_tokens",limit);
        JSONObject response=request("chat/completions",key,body);
        JSONArray choices=response.optJSONArray("choices");JSONObject choice=choices==null?null:choices.optJSONObject(0);
        JSONObject message=choice==null?null:choice.optJSONObject("message");Object text=message==null?null:message.opt("content");
        if(!(text instanceof String)||((String)text).trim().isEmpty()||((String)text).length()>8000)throw new Failure(Code.RESPONSE);
        return ((String)text).trim();
    }
    void test()throws Exception{
        long revision=store.revision();String model=store.model();String key=key();
        if(model.isEmpty())throw new Failure(Code.MODEL_REQUIRED);
        store.invalidateTest();
        completion(key,model,new JSONArray().put(new JSONObject().put("role","user").put("content","Connection test. Reply briefly with OK.")),16);
        if(!store.markTested(revision,model))throw new Failure(Code.CHANGED);
    }
    String explain(JSONObject facts)throws Exception{
        long revision=store.revision();String model=store.model(),key=key();if(model.isEmpty())throw new Failure(Code.MODEL_REQUIRED);
        // Allowlist: never send SSID, IP, serial, location, installed-app list or arbitrary commands.
        JSONObject safe=new JSONObject();
        for(String name:new String[]{"transport","validated","vpn","linkMbps","estimatedDownloadMbps","downloadMeasuredAt"})if(facts.has(name))safe.put(name,facts.get(name));
        JSONArray messages=new JSONArray().put(new JSONObject().put("role","system").put("content",
            "You are Jarvis on FayyadhOS Android TV. Explain only supplied Android network facts in concise Bahasa Melayu, at most 120 words. Wi-Fi link rate is not Internet speed. A download value is an earlier limited estimate, not a current measurement. Upload, latency and packet loss are unmeasured. Do not invent weather, device specifications or test results. You cannot launch apps or change settings. Offer safe button-based advice. No microphone or offline AI."))
            .put(new JSONObject().put("role","user").put("content",safe.toString()));
        String result=completion(key,model,messages,300);if(store.revision()!=revision)throw new Failure(Code.CHANGED);return result;
    }
    void cancel(){cancelled=true;transport.cancel();}
    static String safeError(Exception e){return e instanceof Failure?e.getMessage():e instanceof JarvisKeyStore.StorageFailure?message(Code.STORAGE):e instanceof SocketTimeoutException?message(Code.TIMEOUT):message(Code.NETWORK);}
    static final class DirectTransport implements Transport {
        private volatile HttpsURLConnection connection;private volatile boolean cancelled;
        public Response exchange(URL url,String key,byte[] body)throws Exception{
            if(cancelled)throw new Failure(Code.CANCELLED);
            HttpsURLConnection c=(HttpsURLConnection)url.openConnection();connection=c;
            ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();AtomicBoolean expired=new AtomicBoolean();
            ScheduledFuture<?> deadline=timer.schedule(()->{expired.set(true);c.disconnect();},25,TimeUnit.SECONDS);
            try{
                if(cancelled)throw new Failure(Code.CANCELLED);
                c.setConnectTimeout(5000);c.setReadTimeout(18000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
                c.setRequestProperty("Accept","application/json");c.setRequestProperty("Authorization","Bearer "+key);
                if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setFixedLengthStreamingMode(body.length);try(OutputStream out=c.getOutputStream()){out.write(body);}}
                int status=c.getResponseCode();if(status!=200)return new Response(status,c.getContentType(),""); // Never expose raw provider errors.
                try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[4096];int n;
                    while((n=in.read(buffer))!=-1){if(cancelled||Thread.currentThread().isInterrupted())throw new Failure(Code.CANCELLED);if(out.size()+n>MAX_BYTES)throw new Failure(Code.RESPONSE);out.write(buffer,0,n);}
                    return new Response(status,c.getContentType(),out.toString("UTF-8"));
                }
            }catch(IOException e){if(expired.get())throw new Failure(Code.TIMEOUT);throw e;}
            finally{deadline.cancel(false);timer.shutdownNow();c.disconnect();connection=null;}
        }
        public void cancel(){cancelled=true;HttpsURLConnection c=connection;if(c!=null)c.disconnect();}
    }
}
