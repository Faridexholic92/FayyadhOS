package com.gathin.tvhome;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;
import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import java.io.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MechaFramesTest {
 Context context(){return RuntimeEnvironment.getApplication();}
 JSONObject manifest()throws Exception{try(InputStream in=context().getAssets().open("mini-mecha3d/manifest.json")){return new JSONObject(new String(MechaFrames.readLimited(in,16384),java.nio.charset.StandardCharsets.UTF_8));}}
 @Test public void verifiedNormalAndLowRamAssetsStayWithinDeclaredMemoryBudget()throws Exception{
  for(boolean low:new boolean[]{false,true}){MechaFrames f=MechaFrames.load(context(),low);try{assertEquals(low?2:1,f.sample());assertEquals(MechaFrames.MAX_BYTES/(low?4:1),f.allocatedBytes());Bitmap b=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Rect src=new Rect();RectF dst=new RectF(0,0,112,128);for(int phase=0;phase<3;phase++)f.draw(c,phase,1,phase==1?63:31,src,dst,new Paint(3));assertNotEquals(0,b.getGenerationId());b.recycle();}finally{f.recycle();assertEquals(0,f.allocatedBytes());}}
 }
 @Test public void corruptBytesAndOversizedInputsAreRejected()throws Exception{
  assertThrows(IOException.class,()->MechaFrames.verifyBytes(new byte[]{1,2},2,"0".repeat(64)));assertThrows(IOException.class,()->MechaFrames.readLimited(new ByteArrayInputStream(new byte[65]),64));
 }
 @Test public void manifestRejectsUnexpectedDimensionsCountsAndPartLimits()throws Exception{
  JSONObject m=manifest();m.getJSONArray("atlases").getJSONObject(0).put("width",100000);assertThrows(IOException.class,()->MechaFrames.validateManifest(m));
  JSONObject n=manifest();n.getJSONArray("atlases").getJSONObject(0).put("parts",33);assertThrows(IOException.class,()->MechaFrames.validateManifest(n));
 }
 @Test public void everyTimelineFrameIsInBoundsAndLoopIsDeterministic(){
  for(long t=-20000;t<=40000;t+=7){int phase=MiniMechaView.phaseAt(t);assertTrue(phase>=0&&phase<3);for(int row=0;row<2;row++){int f=MiniMechaView.frameAt(row,t);assertTrue(f>=0&&f<(phase==1?64:32));assertEquals(f,MiniMechaView.frameAt(row,t+20000));}}
  assertEquals(0,MiniMechaView.phaseAt(2500));assertEquals(1,MiniMechaView.phaseAt(6500));assertEquals(2,MiniMechaView.phaseAt(19000));
 }
 @Test public void atlasLoadingIsOffMainThreadAndDoesNotTakeFocus()throws Exception{
  AtomicReference<Thread> worker=new AtomicReference<>();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   MiniMechaView v=new MiniMechaView(a.get(),(c,low)->{worker.set(Thread.currentThread());entered.countDown();release.await(3,TimeUnit.SECONDS);return MechaFrames.load(c,low);});((android.widget.FrameLayout)a.get().findViewById(R.id.root)).addView(v,new android.widget.FrameLayout.LayoutParams(800,96));v.hostActive(true);
   assertTrue(entered.await(2,TimeUnit.SECONDS));assertNotSame(Looper.getMainLooper().getThread(),worker.get());assertTrue(v.loading());assertFalse(v.frameScheduled());assertFalse(v.isFocusable());release.countDown();JarvisRepairTest.await(v::running);assertTrue(v.assetsReady());v.destroy();assertEquals(0,v.allocatedBytes());
  }finally{release.countDown();}
 }
 @Test public void cancelledLoadCannotResurrectAfterOffOrDestroy()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<MechaFrames> returned=new AtomicReference<>();
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   MiniMechaView v=new MiniMechaView(a.get(),(c,low)->{MechaFrames f=MechaFrames.load(c,low);entered.countDown();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(release.getCount()>0&&System.nanoTime()<until){try{release.await(100,TimeUnit.MILLISECONDS);}catch(InterruptedException ignored){}}Thread.interrupted();returned.set(f);return f;});((android.widget.FrameLayout)a.get().findViewById(R.id.root)).addView(v,new android.widget.FrameLayout.LayoutParams(800,96));v.hostActive(true);assertTrue(entered.await(2,TimeUnit.SECONDS));v.featureEnabled(false);v.destroy();release.countDown();JarvisRepairTest.await(()->returned.get()!=null);JarvisRepairTest.await(()->returned.get().allocatedBytes()==0);assertFalse(v.loading());assertFalse(v.frameScheduled());assertEquals(View.GONE,v.getVisibility());
  }finally{release.countDown();}
 }
 @Test public void failedAssetsStayHiddenAndUserToggleCanRetry()throws Exception{
  AtomicInteger attempts=new AtomicInteger();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   MiniMechaView v=new MiniMechaView(a.get(),(c,low)->{if(attempts.getAndIncrement()==0)throw new IOException("Fixture corrupt asset");return MechaFrames.load(c,low);});((android.widget.FrameLayout)a.get().findViewById(R.id.root)).addView(v,new android.widget.FrameLayout.LayoutParams(800,96));v.hostActive(true);JarvisRepairTest.await(v::assetFailed);assertFalse(v.frameScheduled());assertEquals(View.GONE,v.getVisibility());assertTrue(v.menuLabel().contains("aset gagal"));v.featureEnabled(false);v.featureEnabled(true);JarvisRepairTest.await(v::running);v.destroy();
  }
 }
 @Test @org.robolectric.annotation.Config(sdk=26) public void androidEightCanDecodeThePrerenderedAssets()throws Exception{MechaFrames f=MechaFrames.load(context(),true);assertEquals(MechaFrames.MAX_BYTES/4,f.allocatedBytes());f.recycle();}
}
