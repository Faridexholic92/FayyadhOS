package com.gathin.tvhome;
import android.app.Dialog;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class SpeedtestGoTest {
 @Test public void openingAndReopeningNeverCreatesProbeOrAutoStarts()throws Exception{
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=new DashboardTest().render(a.get(),1920,1080);AtomicInteger creates=new AtomicInteger();
   JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->{creates.incrementAndGet();throw new AssertionError("Must not auto-start");});
   panel.show();panel.openSpeedtest();JarvisRepairTest.pump();
   Dialog d=ShadowDialog.getLatestDialog();TextView go=JarvisRepairTest.find(d.getWindow().getDecorView(),"GO");
   assertNotNull(go);assertTrue(go.isEnabled());assertTrue(go.hasFocus());assertEquals(0,creates.get());
   assertEquals(false,JarvisRepairTest.field(panel,"testing"));assertNull(JarvisRepairTest.field(panel,"future"));
   assertTrue(((TextView)JarvisRepairTest.field(panel,"testUsage")).getText().toString().contains("Belum diuji"));
   d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));
   panel.openSpeedtest();JarvisRepairTest.pump();assertEquals(0,creates.get());assertTrue(go.hasFocus());panel.close();
  }
 }
 @Test public void goCanRetryAfterFailureWithoutPublishingFakeMetrics()throws Exception{
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=new DashboardTest().render(a.get(),1920,1080);AtomicInteger creates=new AtomicInteger();
   JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->{
    try{SpeedProbeTest.FakeConnection c=new SpeedProbeTest.FakeConnection();if(creates.getAndIncrement()==0)c.code=503;return new SpeedProbe(()->c);}catch(Exception e){throw new RuntimeException(e);}
   });panel.show();panel.openSpeedtest();Dialog d=ShadowDialog.getLatestDialog();
   JarvisRepairTest.find(d.getWindow().getDecorView(),"GO").performClick();
   JarvisRepairTest.await(()->{try{return !(boolean)JarvisRepairTest.field(panel,"testing");}catch(Exception e){return false;}});
   TextView go=(TextView)JarvisRepairTest.field(panel,"testStart");assertTrue(go.isEnabled());assertEquals("GO",go.getText().toString());assertTrue(go.hasFocus());
   assertNull(JarvisRepairTest.field(panel,"measuredDownload"));
   new JarvisSpeedtestTest().capture(root,d,"08-failure",1920,1080);
   go.performClick();JarvisRepairTest.await(()->{try{return !(boolean)JarvisRepairTest.field(panel,"testing");}catch(Exception e){return false;}});
   assertEquals(2,creates.get());assertNotNull(JarvisRepairTest.field(panel,"measuredDownload"));assertEquals("GO",go.getText().toString());panel.close();
  }
 }
 @Test public void cancelledUiKeepsGoAndClearsTheRing()throws Exception{
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=new DashboardTest().render(a.get(),1920,1080);JarvisSpeedtestTest.Controlled c=new JarvisSpeedtestTest.Controlled();
   JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->new SpeedProbe(()->c));panel.show();panel.openSpeedtest();Dialog d=ShadowDialog.getLatestDialog();
   JarvisRepairTest.find(d.getWindow().getDecorView(),"GO").performClick();JarvisRepairTest.find(d.getWindow().getDecorView(),"Batal ujian").performClick();JarvisRepairTest.pump();
   assertNull(JarvisRepairTest.field(panel,"measuredDownload"));assertEquals(0f,(float)JarvisRepairTest.field(JarvisRepairTest.field(panel,"testMeter"),"progress"),0);
   new JarvisSpeedtestTest().capture(root,d,"09-cancelled",1920,1080);panel.close();
  }
 }
 @Test public void ringTracksTimeAndMinimumDataOrCapNotMandatory100MiB(){
  assertEquals(0,SpeedProbe.sampleProgress(0,0),0);
  assertEquals(0,SpeedProbe.sampleProgress(100,Double.NaN),0);
  assertEquals(.5,SpeedProbe.sampleProgress(SpeedProbe.MIN_SAMPLE_BYTES,5),.001);
  assertEquals(.5,SpeedProbe.sampleProgress(SpeedProbe.MIN_SAMPLE_BYTES/2,10),.001);
  assertEquals(1,SpeedProbe.sampleProgress(SpeedProbe.MIN_SAMPLE_BYTES,10),0);
  assertEquals(1,SpeedProbe.sampleProgress(SpeedProbe.LIMIT,1),0);
  assertEquals(1,SpeedProbe.sampleProgress(SpeedProbe.LIMIT*2L,100),0);
 }
}
