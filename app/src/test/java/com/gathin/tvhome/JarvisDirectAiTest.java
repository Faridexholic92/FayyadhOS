package com.gathin.tvhome;

import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.crypto.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Deterministic transport and AES tests; neither a real Groq key nor Android hardware Keystore. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class JarvisDirectAiTest {
 static final String KEY="fixture-not-a-real-groq-key";
 Context context;JarvisKeyStore store;SecretKey aes;
 static class Wire implements JarvisAiClient.Transport {
  int calls;boolean cancelled;URL url;String key;JSONObject body;int status=200;String type="application/json",reply="{\"choices\":[{\"message\":{\"content\":\"TEST FIXTURE, NOT LIVE\"}}]}";
  Exception failure;Runnable during;
  public JarvisAiClient.Response exchange(URL u,String k,byte[] b)throws Exception{calls++;url=u;key=k;body=b==null?null:new JSONObject(new String(b,java.nio.charset.StandardCharsets.UTF_8));if(failure!=null)throw failure;if(during!=null)during.run();return new JarvisAiClient.Response(status,type,reply);}
  public void cancel(){cancelled=true;}
 }
 @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();context.getSharedPreferences(JarvisKeyStore.PREFS,0).edit().clear().commit();KeyGenerator g=KeyGenerator.getInstance("AES");g.init(256);aes=g.generateKey();store=new JarvisKeyStore(context,()->aes);}
 void configured()throws Exception{store.save(KEY);store.selectModel("account-listed-test-model");}
 JarvisAiClient client(Wire wire){return new JarvisAiClient(store,wire);}
 void fails(JarvisAiClient.Code code,Throwing action)throws Exception{try{action.run();fail("Expected failure");}catch(JarvisAiClient.Failure f){assertEquals(code,f.code);assertFalse(f.getMessage().contains(KEY));}}
 interface Throwing{void run()throws Exception;}
 @Test public void encryptedKeyRoundTripAndNoPlaintextPreference()throws Exception{store.save(KEY);assertEquals(KEY,store.read());assertTrue(store.configured());assertFalse(context.getSharedPreferences(JarvisKeyStore.PREFS,0).getAll().toString().contains(KEY));}
 @Test public void encryptionUsesNewRandomNonceOnReplacement()throws Exception{store.save(KEY);String first=context.getSharedPreferences(JarvisKeyStore.PREFS,0).getString("iv","");store.save(KEY);assertNotEquals(first,context.getSharedPreferences(JarvisKeyStore.PREFS,0).getString("iv",""));}
 @Test public void corruptCiphertextFailsWithoutPlaintextFallback()throws Exception{store.save(KEY);context.getSharedPreferences(JarvisKeyStore.PREFS,0).edit().putString("ciphertext",Base64.encodeToString(new byte[32],Base64.NO_WRAP)).commit();try{store.read();fail();}catch(JarvisKeyStore.StorageFailure e){assertFalse(e.getMessage().contains(KEY));}}
 @Test public void unavailableKeystoreFailsClosed()throws Exception{JarvisKeyStore broken=new JarvisKeyStore(context,()->{throw new IOException(KEY);});try{broken.save(KEY);fail();}catch(JarvisKeyStore.StorageFailure e){assertFalse(e.getMessage().contains(KEY));}assertFalse(broken.configured());}
 @Test public void removalClearsEncryptedMaterialModelAndTestStatus()throws Exception{configured();assertTrue(store.markTested(store.revision(),store.model()));long rev=store.revision();store.remove();assertFalse(store.configured());assertEquals("",store.read());assertEquals("",store.model());assertEquals(0,store.testedAt());assertTrue(store.revision()>rev);}
 @Test public void keyReplacementInvalidatesSelectedModelAndSuccessfulTest()throws Exception{configured();store.markTested(store.revision(),store.model());store.save("replacement-fixture-key");assertEquals("",store.model());assertEquals(0,store.testedAt());}
 @Test public void staleTestCannotMarkNewConfigurationSuccessful()throws Exception{configured();long rev=store.revision();store.selectModel("another-listed-model");assertFalse(store.markTested(rev,"account-listed-test-model"));assertEquals(0,store.testedAt());}
 @Test public void noKeyDoesNotContactAnyEndpoint()throws Exception{Wire w=new Wire();fails(JarvisAiClient.Code.NO_KEY,()->client(w).explain(new JSONObject()));assertEquals(0,w.calls);}
 @Test public void noModelDoesNotRunCompletion()throws Exception{store.save(KEY);Wire w=new Wire();fails(JarvisAiClient.Code.MODEL_REQUIRED,()->client(w).test());assertEquals(0,w.calls);}
 @Test public void modelDiscoveryUsesOfficialHostAndSavedKeyOnly()throws Exception{store.save(KEY);Wire w=new Wire();w.reply="{\"data\":[{\"id\":\"model-b\"},{\"id\":\"model-a\"},{\"id\":\"inactive\",\"active\":false}]}";assertEquals(Arrays.asList("model-a","model-b"),client(w).models());assertEquals("https://api.groq.com/openai/v1/models",w.url.toString());assertEquals(KEY,w.key);assertNull(w.body);assertEquals(0,store.testedAt());}
 @Test public void directCompletionUsesActualChosenModelAndSanitizedFacts()throws Exception{configured();Wire w=new Wire();String text=client(w).explain(new JSONObject().put("transport","Wi-Fi").put("validated",true).put("ssid","PRIVATE-SSID").put("command","arbitrary-command"));assertEquals("TEST FIXTURE, NOT LIVE",text);assertEquals("https://api.groq.com/openai/v1/chat/completions",w.url.toString());assertEquals("account-listed-test-model",w.body.getString("model"));assertFalse(w.body.getBoolean("stream"));assertFalse(w.body.toString().contains("PRIVATE-SSID"));assertFalse(w.body.toString().contains("arbitrary-command"));assertFalse(w.body.toString().contains(KEY));}
 @Test public void connectionTestRequiresRealCompletionResponseNotModelListing()throws Exception{configured();Wire w=new Wire();client(w).test();assertEquals(1,w.calls);assertEquals("/openai/v1/chat/completions",w.url.getPath());assertTrue(store.testedAt()>0);}
 @Test public void eachFailureStatusIsExplicitAndDoesNotExposeProviderBody()throws Exception{configured();int[] statuses={401,403,404,400,422,429,500,503,302};JarvisAiClient.Code[] codes={JarvisAiClient.Code.AUTH,JarvisAiClient.Code.ACCESS,JarvisAiClient.Code.ACCESS,JarvisAiClient.Code.ACCESS,JarvisAiClient.Code.ACCESS,JarvisAiClient.Code.RATE_LIMIT,JarvisAiClient.Code.PROVIDER,JarvisAiClient.Code.PROVIDER,JarvisAiClient.Code.RESPONSE};for(int i=0;i<statuses.length;i++){Wire w=new Wire();w.status=statuses[i];w.reply=KEY;final Wire wire=w;fails(codes[i],()->client(wire).explain(new JSONObject()));}}
 @Test public void malformedWrongTypeEmptyAndOversizedResponsesCannotSucceed()throws Exception{configured();for(String body:new String[]{"not-json","{}","{\"choices\":[]}","{\"choices\":[{\"message\":{\"content\":\"\"}}]}","x".repeat(JarvisAiClient.MAX_BYTES+1)}){Wire w=new Wire();w.reply=body;fails(JarvisAiClient.Code.RESPONSE,()->client(w).explain(new JSONObject()));}Wire html=new Wire();html.type="text/html";fails(JarvisAiClient.Code.RESPONSE,()->client(html).test());assertEquals(0,store.testedAt());}
 @Test public void timeoutAndNetworkFailuresAreDistinctAndSanitized()throws Exception{configured();Wire w=new Wire();w.failure=new SocketTimeoutException(KEY);fails(JarvisAiClient.Code.TIMEOUT,()->client(w).test());w.failure=new UnknownHostException(KEY);fails(JarvisAiClient.Code.NETWORK,()->client(w).test());assertFalse(JarvisAiClient.safeError(new IOException(KEY)).contains(KEY));}
 @Test public void cancelPreventsRequestsAndCancelsTransport()throws Exception{configured();Wire w=new Wire();JarvisAiClient c=client(w);c.cancel();fails(JarvisAiClient.Code.CANCELLED,()->c.test());assertEquals(0,w.calls);assertTrue(w.cancelled);}
 @Test public void configurationChangeDuringRequestRejectsStaleResponse()throws Exception{configured();Wire w=new Wire();w.during=()->{try{store.remove();}catch(Exception e){throw new RuntimeException(e);}};fails(JarvisAiClient.Code.CHANGED,()->client(w).explain(new JSONObject()));assertEquals(0,store.testedAt());}
}
