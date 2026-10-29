package com.gathin.tvhome;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.provider.Settings;import android.view.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import java.time.Duration;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MiniMechaTest {
 DashboardTest helper=new DashboardTest();
 MiniMechaView robot(DashboardActivity a){return a.findViewById(R.id.root).findViewWithTag("mini-mecha");}
 void tick(long ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @Test public void decorativeLaneDoesNotTakeRemoteOrTouchAndStaysOutsideContent()throws Exception{
  helper.demoApps();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=helper.render(a.get(),1920,1080);MiniMechaView v=robot(a.get());View focus=root.findFocus();
   assertFalse(v.isFocusable());assertFalse(v.isClickable());assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO,v.getImportantForAccessibility());assertFalse(v.requestFocus());assertSame(focus,root.findFocus());
   MotionEvent event=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,5,5,0);assertFalse(v.dispatchTouchEvent(event));event.recycle();
   View dock=root.findViewWithTag("glass-dock"),weather=root.findViewWithTag("hourly-weather"),greeting=root.findViewWithTag("greeting");
   assertTrue(v.getBottom()<dock.getTop());assertTrue(v.getTop()>weather.getBottom());assertTrue(v.getTop()>((View)greeting.getParent()).getBottom());assertTrue(v.running());tick(200);assertTrue(v.playhead()>0);
  }
 }
 @Test public void pauseScreenOffAndNestedMenusCancelCallbacksAndResumeSafely()throws Exception{
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   helper.render(a.get(),1920,1080);MiniMechaView v=robot(a.get());assertTrue(v.running());
   AlertDialog one=a.get().show(new AlertDialog.Builder(a.get()).setTitle("One"));AlertDialog two=a.get().show(new AlertDialog.Builder(a.get()).setTitle("Two"));assertFalse(v.frameScheduled());assertEquals(View.GONE,v.getVisibility());
   one.dismiss();tick(60);assertFalse(v.frameScheduled());two.dismiss();tick(60);assertTrue(v.running());
   a.get().sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));tick(60);long stopped=v.callbacks;tick(300);assertEquals(stopped,v.callbacks);assertFalse(v.frameScheduled());
   a.get().sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));tick(60);assertTrue(v.running());
   a.pause();stopped=v.callbacks;tick(300);assertEquals(stopped,v.callbacks);a.resume();assertTrue(v.running());v.destroy();stopped=v.callbacks;tick(300);assertEquals(stopped,v.callbacks);
  }
 }
 @Test public void offPersistsWithoutAffectingWeatherOrArtwork()throws Exception{
  Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("hourly-weather",0).edit().putString("label","Keep weather").commit();c.getSharedPreferences(AppArtwork.PREFS,0).edit().putString("pack","keep.pack").commit();
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   helper.render(a.get(),1920,1080);MiniMechaView v=robot(a.get());a.get().settings();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals("Mini robot · ON",d.getListView().getItemAtPosition(16));d.getListView().performItemClick(null,16,16);d.dismiss();tick(60);assertFalse(v.featureEnabled());assertFalse(v.frameScheduled());assertEquals(View.GONE,v.getVisibility());
  }
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){helper.render(a.get(),1920,1080);assertFalse(robot(a.get()).featureEnabled());assertFalse(robot(a.get()).running());}
  assertEquals("Keep weather",c.getSharedPreferences("hourly-weather",0).getString("label",""));assertEquals("keep.pack",c.getSharedPreferences(AppArtwork.PREFS,0).getString("pack",""));
 }
 @Test public void reducedMotionDisablesAnimationAndLowRamUsesTenFps()throws Exception{
  Context c=RuntimeEnvironment.getApplication();Settings.Global.putFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,0);
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){helper.render(a.get(),1920,1080);assertFalse(robot(a.get()).frameScheduled());assertEquals(View.GONE,robot(a.get()).getVisibility());}
  Settings.Global.putFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1);Shadows.shadowOf((ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE)).setIsLowRamDevice(true);
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){helper.render(a.get(),1920,1080);assertEquals(100,robot(a.get()).frameInterval());assertTrue(robot(a.get()).running());}
 }
 @Test public void pathsAreBoundedContinuousAndLoopWithoutTeleport(){
  for(long t=0;t<20000;t+=10){float x=MiniMechaView.whitePosition(t);assertTrue(x>=.079f&&x<=.551f);assertTrue(x+.27f<1);assertTrue(Math.abs(x-MiniMechaView.whitePosition(t+10))<.002f);}
  assertEquals(MiniMechaView.whitePosition(0),MiniMechaView.whitePosition(20000),0);assertTrue(MiniMechaView.runningPhase(2000));assertFalse(MiniMechaView.runningPhase(7000));
 }
 void preview(int w,int h,String suffix)throws Exception{
  helper.demoApps();helper.seedApiCache();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=helper.render(a.get(),w,h);MiniMechaView v=robot(a.get());assertTrue(v.getBottom()<((View)root.findViewWithTag("glass-dock")).getTop());assertNotNull(root.findFocus());
   v.previewAt(2500);helper.capture(root,"mecha/run-"+suffix);v.previewAt(6500);helper.capture(root,"mecha/duel-"+suffix);
  }
 }
 @Test public void preview1080()throws Exception{new java.io.File(System.getProperty("user.dir"),"build/mecha").mkdirs();preview(1920,1080,"1080");}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void preview720()throws Exception{new java.io.File(System.getProperty("user.dir"),"build/mecha").mkdirs();preview(1280,720,"720");}
 @Test public void captureAndroidAnimationFrames()throws Exception{
  helper.demoApps();helper.seedApiCache();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   View root=helper.render(a.get(),1920,1080);MiniMechaView v=robot(a.get());java.io.File folder=new java.io.File(System.getProperty("user.dir"),"build/mecha/video");folder.mkdirs();
   v.featureEnabled(false);helper.capture(root,"mecha/video/base");v.featureEnabled(true);
   Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
   for(int i=0;i<400;i++){b.eraseColor(Color.TRANSPARENT);v.previewAt(i*50L);v.draw(c);try(var out=new java.io.FileOutputStream(new java.io.File(folder,String.format(java.util.Locale.ROOT,"%03d.png",i)))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
   b.recycle();assertTrue(v.running());
  }
 }

}
