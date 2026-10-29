package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RainFeatureTest {
 final DashboardTest helper=new DashboardTest();
 static final class Fake implements RainController.Source {
  WeatherForecast cached;Exception failure;AtomicInteger calls=new AtomicInteger();
  Fake(String text,long age)throws Exception{cached=WeatherForecastTest.data(text,age);}
  public WeatherForecast cached(String id){return cached;}
  public WeatherForecast fetch(String id,long now)throws Exception{calls.incrementAndGet();if(failure!=null)throw failure;return cached;}
 }
 void idle(long ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 void waitWeather(RainController r)throws Exception{for(int i=0;i<200&&r.isBusy();i++){Thread.sleep(5);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertFalse("Weather worker timed out",r.isBusy());}
 void prefs(boolean forecast){RainOptions.prefs(RuntimeEnvironment.getApplication()).edit().putBoolean("kinarut_default_v1",true).putBoolean("enabled",true).putBoolean("forecast",forecast).putString("location_id",WeatherForecastTest.ID).putString("location_name","Kota Kinabalu").commit();}
 RainController attach(DashboardActivity a,Fake source,boolean online)throws Exception{
  a.rain.destroy();RainController r=new RainController(a,a.findViewById(R.id.root),source,Clock.fixed(WeatherForecastTest.NOW,ZoneOffset.UTC),()->online);a.rain=r;r.resume();waitWeather(r);return r;
 }
 @Test public void disabledPreferenceHasNoRainFramesButHomeForecastMaySchedule()throws Exception{RainOptions.prefs(RuntimeEnvironment.getApplication()).edit().putBoolean("enabled",false).commit();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=c.get().rain;waitWeather(r);idle(3000);assertFalse(r.view.hasScheduledFrame());assertTrue("Home forecast schedules independently of disabled rain",r.hasWeatherWake());assertEquals(0,r.view.frameCallbacks);assertEquals(0,r.view.spriteBytes());assertFalse(r.view.isFocusable());assertFalse(r.view.isClickable());assertEquals(2,((FrameLayout)c.get().findViewById(R.id.root)).getChildCount());
 }}
 @Test public void enableAloneDoesNotSilentlyEnableForecast()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);Fake f=new Fake("Hujan",0);RainController r=attach(c.get(),f,true);assertEquals(0,f.calls.get());assertFalse(r.hasWeatherWake());assertFalse(r.view.effectVisible());
 }}
 @Test public void clearAndCloudyDoNotRender()throws Exception{prefs(true);for(String state:new String[]{"Tiada Hujan","Berawan"})try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=attach(c.get(),new Fake(state,0),true);idle(1000);assertFalse(r.view.effectVisible());assertFalse(r.view.hasScheduledFrame());
 }}
 @Test public void rainAndThunderForecastActivateButFreshCacheAvoidsNetwork()throws Exception{prefs(true);for(String state:new String[]{"Hujan","Ribut petir"})try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);Fake f=new Fake(state,0);RainController r=attach(c.get(),f,true);idle(900);assertTrue(r.view.effectVisible());assertEquals(0,f.calls.get());assertTrue(r.hasWeatherWake());assertTrue(r.view.spriteBytes()<140000);
 }}
 @Test public void offlineRetainsFreshCacheButExpiresSafely()throws Exception{prefs(true);for(long age:new long[]{7*3600000L,25*3600000L})try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);Fake f=new Fake("Hujan",age);RainController r=attach(c.get(),f,false);idle(900);assertEquals(0,f.calls.get());assertEquals(age<WeatherForecast.FRESH_MILLIS,r.view.effectVisible());assertTrue(r.nextFetchAt()>WeatherForecastTest.NOW.toEpochMilli());
 }}
 @Test public void invalidNetworkResponseRetainsFreshLastStateAndBacksOff()throws Exception{prefs(true);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);Fake f=new Fake("Hujan",7*3600000L);f.failure=new IllegalArgumentException("Invalid API response");RainController r=attach(c.get(),f,true);idle(800);assertEquals(1,f.calls.get());assertTrue(r.view.effectVisible());r.refresh();waitWeather(r);assertEquals(1,f.calls.get());assertTrue(r.status().contains("cache retained"));
 }}
 @Test public void rateLimitHonorsRetryAfter()throws Exception{prefs(true);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);Fake f=new Fake("Tiada Hujan",7*3600000L);f.failure=new MetWeatherRepository.FetchFailure(429,3600000);RainController r=attach(c.get(),f,true);assertEquals(WeatherForecastTest.NOW.toEpochMilli()+3600000,r.nextFetchAt());r.refresh();waitWeather(r);assertEquals(1,f.calls.get());
 }}
 @Test public void previewIsSeparateAndTurningItOffRestoresClearState()throws Exception{prefs(true);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=attach(c.get(),new Fake("Tiada Hujan",0),true);r.preview(true);idle(800);assertTrue(r.view.effectVisible());assertEquals(WeatherForecast.State.CLEAR,r.currentState());r.preview(false);idle(900);assertFalse(r.view.effectVisible());assertFalse(r.view.hasScheduledFrame());assertEquals(0,r.view.spriteBytes());
 }}
 @Test public void backgroundAndScreenOffSuspendAndPreviewDoesNotPersist()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=attach(c.get(),new Fake("Hujan",0),false);r.preview(true);idle(800);c.pause();long n=r.view.frameCallbacks;idle(5000);assertEquals(n,r.view.frameCallbacks);assertFalse(r.previewing());assertEquals(0,r.view.spriteBytes());c.resume();idle(800);assertFalse(r.view.effectVisible());r.preview(true);idle(800);r.screen(false);n=r.view.frameCallbacks;idle(3000);assertEquals(n,r.view.frameCallbacks);assertFalse(r.view.hasScheduledFrame());
 }}
 @Test public void forecastRainResumesAfterBackground()throws Exception{prefs(true);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=attach(c.get(),new Fake("Hujan",0),true);idle(800);c.pause();assertFalse(r.view.effectVisible());c.resume();waitWeather(r);idle(800);assertTrue(r.view.effectVisible());
 }}
 @Test public void staticModeStopsFramesAndActiveSettingsRespectCaps()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=c.get().rain;r.preview(true);idle(800);RainOptions.prefs(c.get()).edit().putInt("density",999).putBoolean("moving",false).commit();idle(1000);assertEquals(96,r.view.dropletCount());assertFalse(r.view.hasScheduledFrame());long n=r.view.frameCallbacks;idle(3000);assertEquals(n,r.view.frameCallbacks);RainOptions.prefs(c.get()).edit().putBoolean("enabled",false).commit();assertFalse(r.view.effectVisible());assertEquals(0,r.view.spriteBytes());
 }}
 @Test public void modalMenusSuspendCosmeticAnimation()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainController r=c.get().rain;r.preview(true);idle(800);c.get().settings();assertFalse(r.view.effectVisible());assertFalse(r.view.hasScheduledFrame());org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().dismiss();idle(800);assertTrue(r.view.effectVisible());
 }}
 @Test public void remoteAndApplicationLaunchStillWorkThroughRain()throws Exception{prefs(false);helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  View root=helper.render(c.get(),1920,1080);c.get().rain.preview(true);idle(800);RecyclerView grid=root.findViewById(R.id.grid);View first=grid.getChildAt(0);first.requestFocus();View next=first.focusSearch(View.FOCUS_RIGHT);assertNotNull(next);next.requestFocus();assertTrue(next.hasFocus());root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));idle(150);assertNotNull(Shadows.shadowOf(c.get()).getNextStartedActivity());assertFalse(c.get().rain.view.onTouchEvent(MotionEvent.obtain(0,1,MotionEvent.ACTION_DOWN,1,1,0)));
 }}
 @Test public void manualPreviewRenders720And1080WithoutChangingHome()throws Exception{prefs(false);helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=helper.render(c.get(),1920,1080);helper.capture(root,"rain-before-1080");RainOptions.prefs(c.get()).edit().putInt("intensity",2).putInt("density",2).commit();c.get().rain.preview(true);idle(900);helper.capture(root,"rain-preview-1080");assertTrue(c.get().rain.view.effectVisible());
 }}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void previewAt720()throws Exception{prefs(false);helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=helper.render(c.get(),1280,720);c.get().rain.preview(true);idle(800);helper.capture(root,"rain-preview-720");assertEquals(1280,c.get().rain.view.getWidth());assertEquals(720,c.get().rain.view.getHeight());
 }}
 @Test public void settingsEntryIsAddedWithoutReorderingOldItems(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  c.get().settings();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(17,d.getListView().getCount());assertEquals("Mini robot · ON",d.getListView().getItemAtPosition(16));assertEquals("Azan · Lima waktu",d.getListView().getItemAtPosition(8));assertEquals("Wallpaper",d.getListView().getItemAtPosition(0));assertEquals("Set default Home",d.getListView().getItemAtPosition(6));assertEquals("Rain-on-Glass · Weather",d.getListView().getItemAtPosition(7));
 }}
 @Test public void catalogueComesFromOfficialSnapshotAndDefaultIsVerifiedPapar()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  try(var in=c.get().getAssets().open("met-malaysia-locations.json")){org.json.JSONArray rows=new org.json.JSONArray(MetWeatherRepository.read(in));assertEquals(440,rows.length());}assertEquals(RainOptions.HOME_ID,new RainOptions(RainOptions.prefs(c.get())).locationId);
 }}
 @Test @org.robolectric.annotation.Config(sdk=26) public void minimumSdkPreviewWorksWithoutOverlayPermission()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);c.get().rain.preview(true);waitWeather(c.get().rain);idle(800);assertTrue(c.get().rain.view.effectVisible());assertTrue("Home forecast remains active in rain preview",c.get().rain.hasWeatherWake());
 }}
 @Test public void settingsCanEnableAndStartManualPreview()throws Exception{RainOptions.prefs(RuntimeEnvironment.getApplication()).edit().putBoolean("enabled",false).commit();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=helper.render(c.get(),1920,1080);c.get().settings();AlertDialog old=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();old.getListView().performItemClick(null,7,7);old.dismiss();idle(5);
  AlertDialog rain=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(11,rain.getListView().getCount());assertEquals("Home remains visible behind settings",View.VISIBLE,((FrameLayout)root).getChildAt(1).getVisibility());rain.getListView().performItemClick(null,0,0);rain.dismiss();idle(5);
  rain=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();rain.getListView().performItemClick(null,7,7);rain.dismiss();idle(900);assertTrue(c.get().rain.previewing());assertTrue(c.get().rain.view.effectVisible());assertEquals(View.VISIBLE,((FrameLayout)root).getChildAt(1).getVisibility());
 }}
 @Test public void turningEffectOffRestoresExactlyTheOriginalHomePixels()throws Exception{prefs(false);helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=helper.render(c.get(),1920,1080);Bitmap before=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888),after=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888);
  // Compare the independent decorative animation at the same playhead, without masking any pixels.
  MiniMechaView mecha=root.findViewWithTag("mini-mecha");mecha.previewAt(0);root.draw(new Canvas(before));c.get().rain.preview(true);idle(800);c.get().rain.preview(false);idle(900);mecha.previewAt(0);root.draw(new Canvas(after));assertTrue("Home changed outside overlay",before.sameAs(after));before.recycle();after.recycle();
 }}
 @Test public void adaptiveQualityReducesMovingDropsThenStopsContinuousFrames()throws Exception{prefs(false);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  helper.render(c.get(),1920,1080);RainGlassView v=c.get().rain.view;c.get().rain.preview(true);idle(800);java.lang.reflect.Method cost=RainGlassView.class.getDeclaredMethod("noteCost",long.class);cost.setAccessible(true);for(int i=0;i<10;i++)cost.invoke(v,5_000_000L);assertEquals(1,v.activeMovingCount());assertEquals(100,v.frameInterval);for(int i=0;i<10;i++)cost.invoke(v,5_000_000L);idle(200);assertEquals(0,v.activeMovingCount());assertFalse(v.hasScheduledFrame());
 }}

}
