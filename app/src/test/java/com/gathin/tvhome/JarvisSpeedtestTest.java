package com.gathin.tvhome;
import android.app.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowDialog;
import java.io.*;import java.util.concurrent.*;
@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class JarvisSpeedtestTest {
 final DashboardTest helper=new DashboardTest();
 static class Controlled extends SpeedProbeTest.FakeConnection {
  final CountDownLatch release=new CountDownLatch(1);boolean paused;Controlled()throws Exception{}
  public InputStream getInputStream(){InputStream base=super.getInputStream();return new InputStream(){public int read()throws IOException{return base.read();}public int read(byte[] b,int off,int len)throws IOException{
   if(read>=32L*1024*1024&&!paused){paused=true;try{Thread.sleep(300);}catch(InterruptedException e){throw new InterruptedIOException();}}
   if(read>=33L*1024*1024){try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){throw new InterruptedIOException();}}
   return base.read(b,off,len);
  }};}
  public void disconnect(){super.disconnect();release.countDown();}
 }
 void click(Dialog d,String text){TextView b=JarvisRepairTest.find(d.getWindow().getDecorView(),text);assertNotNull(text,b);b.performClick();JarvisRepairTest.pump();}
 void capture(View root,Dialog dialog,String name,int w,int h)throws Exception{
  Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);root.draw(canvas);
  if(dialog!=null){View decor=dialog.getWindow().getDecorView();WindowManager.LayoutParams a=dialog.getWindow().getAttributes();decor.measure(View.MeasureSpec.makeMeasureSpec(a.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(a.height,View.MeasureSpec.EXACTLY));decor.layout(0,0,a.width,a.height);canvas.save();canvas.translate(w-a.x-a.width,a.y);decor.draw(canvas);canvas.restore();}
  Paint p=new Paint(3);p.setColor(0xdd041522);canvas.drawRect(0,h-42,w*.74f,h,p);p.setColor(Color.WHITE);p.setTextSize(w/1920f*20);canvas.drawText("PRATONTON SIMULASI · UI daripada kod Android · aplikasi/data contoh · bukan ujian internet sebenar",30,h-14,p);
  File f=new File(System.getProperty("user.dir"),"build/speedtest-previews/"+name+".png");f.getParentFile().mkdirs();try(var out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
 }
 @Test public void fullFlowWithFixtureProgressResultAndBackRestoresHome()throws Exception{
  helper.demoApps();helper.seedApiCache();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=helper.render(a.get(),1920,1080);View grid=root.findViewById(R.id.grid);int before=grid.getWidth();capture(root,null,"01-home",1920,1080);
   Controlled connection=new Controlled();JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->new SpeedProbe(()->connection));panel.show();Dialog d=ShadowDialog.getLatestDialog();capture(root,d,"02-jarvis",1920,1080);
   click(d,"Speedtest");assertEquals(View.GONE,((View)JarvisRepairTest.field(panel,"mainBox")).getVisibility());capture(root,d,"03-ready",1920,1080);
   click(d,"Mula Speedtest");assertFalse(((TextView)JarvisRepairTest.field(panel,"testStart")).isEnabled());assertTrue(((TextView)JarvisRepairTest.field(panel,"testCancel")).hasFocus());
   JarvisRepairTest.await(()->{try{return (long)JarvisRepairTest.field(panel,"testBytes")>0;}catch(Exception e){return false;}});capture(root,d,"04-running",1920,1080);
   connection.release.countDown();JarvisRepairTest.await(()->{try{return !(boolean)JarvisRepairTest.field(panel,"testing");}catch(Exception e){return false;}});
   assertEquals("100 / 100 MiB",((TextView)JarvisRepairTest.field(panel,"testUsage")).getText().toString());assertTrue(((TextView)JarvisRepairTest.field(panel,"testState")).getText().toString().startsWith("Selesai"));assertNotNull(JarvisRepairTest.field(panel,"measuredDownload"));capture(root,d,"05-result-FIXTURE",1920,1080);
   d.dispatchKeyEvent(new KeyEvent(0,KeyEvent.KEYCODE_BACK));d.dispatchKeyEvent(new KeyEvent(1,KeyEvent.KEYCODE_BACK));assertTrue(d.isShowing());assertTrue(((View)JarvisRepairTest.field(panel,"speedButton")).hasFocus());
   click(d,"Close");helper.render(a.get(),1920,1080);assertEquals(before,grid.getWidth());capture(root,null,"06-closed",1920,1080);
  }
 }
 @Test public void cancelClearsResultAndStaleProgressCannotRestoreSuccess()throws Exception{try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  helper.render(a.get(),1920,1080);Controlled connection=new Controlled();JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->new SpeedProbe(()->connection));panel.show();Dialog d=ShadowDialog.getLatestDialog();click(d,"Speedtest");click(d,"Mula Speedtest");click(d,"Batal ujian");connection.release.countDown();Thread.sleep(100);JarvisRepairTest.pump();assertEquals("Ujian dibatalkan",((TextView)JarvisRepairTest.field(panel,"testState")).getText().toString());assertNull(JarvisRepairTest.field(panel,"measuredDownload"));assertEquals("—\nMbps",((TextView)JarvisRepairTest.field(panel,"testValue")).getText().toString());assertTrue(((TextView)JarvisRepairTest.field(panel,"testStart")).isEnabled());panel.close();
 }}
 @Test public void failureDoesNotPublishSpeed()throws Exception{try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  helper.render(a.get(),1920,1080);SpeedProbeTest.FakeConnection conn=new SpeedProbeTest.FakeConnection();conn.code=503;JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->new SpeedProbe(()->conn));panel.show();Dialog d=ShadowDialog.getLatestDialog();click(d,"Speedtest");click(d,"Mula Speedtest");JarvisRepairTest.await(()->{try{return !(boolean)JarvisRepairTest.field(panel,"testing");}catch(Exception e){return false;}});assertNull(JarvisRepairTest.field(panel,"measuredDownload"));assertEquals("—\nMbps",((TextView)JarvisRepairTest.field(panel,"testValue")).getText().toString());panel.close();
 }}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void speedtest720FitsAndBackCancels()throws Exception{try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  View root=helper.render(a.get(),1280,720);Controlled conn=new Controlled();JarvisPanel panel=new JarvisPanel(a.get(),new JarvisAiClient(a.get()),()->new SpeedProbe(()->conn));panel.show();Dialog d=ShadowDialog.getLatestDialog();click(d,"Speedtest");capture(root,d,"07-ready-720",1280,720);assertTrue(d.getWindow().getAttributes().height+d.getWindow().getAttributes().y<=720);click(d,"Mula Speedtest");d.dispatchKeyEvent(new KeyEvent(0,4));d.dispatchKeyEvent(new KeyEvent(1,4));assertEquals(false,JarvisRepairTest.field(panel,"testing"));assertTrue(d.isShowing());panel.close();
 }}
}
