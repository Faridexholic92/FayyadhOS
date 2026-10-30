package com.gathin.tvhome;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.GraphicsMode;
import java.io.*;import java.lang.reflect.*;import java.time.*;import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PremiumHomeTest {
 final DashboardTest helper=new DashboardTest();
 @Before public void defaults(){Context c=RuntimeEnvironment.getApplication();PremiumTheme.prefs(c).edit().clear().putBoolean("birthday_auto",false).commit();RainOptions.prefs(c).edit().putBoolean("moving",true).commit();Settings.Global.putFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1);}
 PremiumSceneView scene(DashboardActivity a){return a.findViewById(R.id.root).findViewWithTag("premium-scene");}
 void field(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 @Test public void birthdayUsesCleanBackdropForSingleAnimatedCharacterLayer(){
  assertEquals("premium/world.webp",PremiumTheme.asset(true));
  assertEquals("premium/world.webp",PremiumTheme.asset(false));
 }
 @Test public void datedBirthdayNeverRepeatsNextYear(){assertTrue(PremiumTheme.birthday(PremiumTheme.WORLD,true,LocalDate.of(2026,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.WORLD,true,LocalDate.of(2027,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.WORLD,false,LocalDate.of(2026,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.CLASSIC,true,LocalDate.of(2026,10,30)));assertTrue(PremiumTheme.birthday(PremiumTheme.BIRTHDAY,false,LocalDate.of(2026,10,29)));}
 @Test public void packagedArtAndAtlasHaveBoundedDecodedSize()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  for(String asset:new String[]{"premium/world.webp","premium/birthday.webp","premium/heroes-run.webp"})try(InputStream in=c.get().getAssets().open(asset)){Bitmap b=BitmapFactory.decodeStream(in);assertNotNull(asset,b);assertTrue(b.getWidth()<=1920);assertTrue(b.getHeight()<=1080);if(asset.contains("heroes")){assertEquals(1152,b.getWidth());assertEquals(640,b.getHeight());assertTrue(b.hasAlpha());}b.recycle();}
 }}
 @Test public void motionStopsForOverlayPauseScreenAudioAndDestroy(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  PremiumSceneView s=scene(c.get());assertTrue(s.hasFrame());s.obscured(true);assertFalse(s.hasFrame());s.obscured(false);assertTrue(s.hasFrame());s.screen(false);assertFalse(s.hasFrame());s.screen(true);assertTrue(s.hasFrame());s.audioBusy(true);assertFalse(s.hasFrame());s.audioBusy(false);assertTrue(s.hasFrame());s.hostActive(false);assertFalse(s.hasFrame());s.hostActive(true);assertTrue(s.hasFrame());s.destroy();assertFalse(s.hasFrame());assertFalse(s.canAnimate());
 }}
 @Test public void offLiteAndSystemReducedMotionAreHonoured(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  PremiumSceneView s=scene(c.get());s.configure(true,false,0);assertFalse(s.hasFrame());s.configure(true,false,1);assertTrue(s.hasFrame());Settings.Global.putFloat(c.get().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,0);s.configure(true,false,2);assertFalse(s.hasFrame());Settings.Global.putFloat(c.get().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1);RainOptions.prefs(c.get()).edit().putBoolean("moving",false).commit();s.configure(true,false,2);assertFalse(s.hasFrame());
 }}
 @Test public void azanPlaybackUsesPrivateInProcessObserverAndRemovesIt()throws Exception{
  final java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();Runnable callback=calls::incrementAndGet;AzanService.observePlayback(callback);
  var service=Robolectric.buildService(AzanService.class).create();Method state=AzanService.class.getDeclaredMethod("playbackState",boolean.class);state.setAccessible(true);
  try{state.invoke(service.get(),true);assertTrue(AzanService.playing());assertEquals(1,calls.get());AzanService.unobservePlayback(callback);state.invoke(service.get(),false);assertFalse(AzanService.playing());assertEquals(1,calls.get());}finally{AzanService.unobservePlayback(callback);service.destroy();}
 }
 @Test public void classicKeepsSelectedWallpaperAndDisablesSpriteLoop(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  DashboardActivity a=c.get();PremiumTheme.prefs(a).edit().putString("theme",PremiumTheme.CLASSIC).commit();a.setAutomaticWallpaper(false);assertEquals("manual:"+FayyadhWallpapers.DEFAULT,a.currentWallpaperKey());assertFalse(scene(a).hasFrame());
 }}
 @Test public void premiumWeatherNeverTurnsUnknownIntoRain(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  for(WeatherForecast.State state:WeatherForecast.State.values()){c.get().weatherWallpaper(state);assertEquals("premium:"+state.name(),c.get().currentWallpaperKey());}
  c.get().weatherWallpaper(WeatherForecast.State.UNKNOWN);assertTrue(c.get().currentWallpaperKey().endsWith("UNKNOWN"));
 }}
 @Test public void overlayAndThemeSettingsPreserveRemoteFocus(){helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  DashboardActivity a=c.get();View root=helper.render(a,1920,1080);View before=root.findFocus();assertNotNull(before);a.themeSettings();assertFalse(scene(a).hasFrame());AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(5,dialog.getListView().getCount());dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Scene resumes after dialog dismissal",scene(a).hasFrame());assertSame(before,root.findFocus());
 }}
 @Test public void noZoneMeansNoInventedTimeAndExplicitAzanOff(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  TextView next=c.get().findViewById(R.id.root).findViewWithTag("next-prayer");assertTrue(next.getText().toString().contains("Select zone"));TextView azan=c.get().findViewById(R.id.root).findViewWithTag("azan-status");assertEquals("AZAN OFF",azan.getText().toString());
 }}
 void preview(int w,int h,String name,boolean birthday,boolean frames)throws Exception{
  helper.demoApps();Context context=RuntimeEnvironment.getApplication();PremiumTheme.prefs(context).edit().putString("theme",birthday?PremiumTheme.BIRTHDAY:PremiumTheme.WORLD).commit();
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   DashboardActivity a=c.get();View root=helper.render(a,w,h);
   // Inject labelled test-only prayer fixture with a fixed clock. No production demo data.
   PrayerPanel old=root.findViewById(R.id.prayerPanel);old.destroy();ViewGroup parent=(ViewGroup)old.getParent();ViewGroup.LayoutParams params=old.getLayoutParams();parent.removeView(old);
   PrayerWeatherUpgradeTest fixtures=new PrayerWeatherUpgradeTest();PrayerPanel prayer=fixtures.configured(a,new PrayerWeatherUpgradeTest.Time());prayer.dashboardScale(w/1920f);parent.addView(prayer,params);field(a,"prayer",prayer);prayer.start();
   WeatherWidget weather=root.findViewWithTag("weather-widget");weather.update(WeatherForecastTest.data("Hujan",0),WeatherForecastTest.ID,"Papar · fixture",WeatherForecastTest.NOW,true);
   a.weatherWallpaper(WeatherForecast.State.RAIN_EXPECTED);field(scene(a),"elapsed",2500L);
   ((TextView)root.findViewWithTag("greeting")).setText("UI SIMULATION · test apps / schedule / weather · not a physical TV capture");
   root=helper.render(a,w,h);View grid=root.findViewById(R.id.grid);assertTrue(grid.getTop()>h*.8);assertTrue(grid.getBottom()<h*.96);assertTrue(prayer.getBottom()<h*.34);
   ViewGroup hourlyRow=(ViewGroup)((ViewGroup)root.findViewWithTag("hourly-weather")).getChildAt(0);
   for(int i=0;i<3;i++){ViewGroup column=(ViewGroup)hourlyRow.getChildAt(i);TextView title=(TextView)column.getChildAt(0);assertTrue("Compact weather heading fits",title.getPaint().measureText(title.getText().toString())<=column.getWidth());}

   assertEquals(Color.TRANSPARENT,((android.graphics.drawable.GradientDrawable)prayer.getBackground()).getColor().getDefaultColor());
   helper.capture(root,name);
   if(frames){for(int i=0;i<90;i++){field(scene(a),"elapsed",i*100L);helper.capture(root,String.format(java.util.Locale.ROOT,"premium-motion/frame-%03d",i));}}
  }
 }
 @Test public void native1080()throws Exception{preview(1920,1080,"premium-home-1080",false,false);}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void native720WithMotionFrames()throws Exception{preview(1280,720,"premium-home-720",false,true);}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void native4k()throws Exception{preview(3840,2160,"premium-home-4k",false,false);}
 @Test public void birthday1080()throws Exception{preview(1920,1080,"premium-birthday-1080",true,false);}

 @Test public void birthdaySceneContinuesAndDrawsSpritesAfterConfettiWindow()throws Exception {
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   PremiumSceneView s=scene(c.get());
   s.configure(true,true,2);
   field(s,"partyStarted",SystemClock.uptimeMillis()-11_000L);
   assertTrue("Confetti expiry must not stop the Birthday character loop",s.canAnimate());
   field(s,"elapsed",1_000L);
   s.layout(0,0,960,540);
   Bitmap frame=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
   try{
    s.draw(new Canvas(frame));
    int papaPixels=0;
    for(int y=170;y<415;y++)for(int x=285;x<460;x++)if(Color.alpha(frame.getPixel(x,y))>0)papaPixels++;
    int boyPixels=0;
    for(int y=220;y<420;y++)for(int x=520;x<700;x++)if(Color.alpha(frame.getPixel(x,y))>0)boyPixels++;
    assertTrue("Birthday theme must keep drawing PapaZola after confetti expires",papaPixels>100);
    assertTrue("Birthday theme must keep drawing BoBoiBoy after confetti expires",boyPixels>100);
   }finally{frame.recycle();}
  }
 }
 @Test public void freeRunPathsStayInsideTheActionBandAndUseDepthScale() {
  for(int character=0;character<2;character++)for(long t=0;t<FreeRunChoreography.LOOP_MS;t+=250){
   FreeRunChoreography.Pose p=FreeRunChoreography.sample(t,character);
   assertTrue("x is kept on-screen",p.x>=92f&&p.x<=868f);
   assertTrue("ground stays clear of top widgets and app dock",p.groundY>=330f&&p.groundY<=423f);
   assertTrue("depth scale stays bounded",p.scale>=0.75f&&p.scale<=0.99f);
   assertTrue("jump offset stays bounded",p.hop>=0f&&p.hop<=16.1f);
   assertTrue("direction is finite",Float.isFinite(p.directionX));
  }
 }
 @Test public void freeRunPathIsContinuousAcrossLoopBoundary() {
  for(int character=0;character<2;character++){
   FreeRunChoreography.Pose before=FreeRunChoreography.sample(FreeRunChoreography.LOOP_MS-1,character);
   FreeRunChoreography.Pose after=FreeRunChoreography.sample(1,character);
   assertTrue("horizontal wrap must not teleport",Math.abs(before.x-after.x)<1.0f);
   assertTrue("vertical wrap must not teleport",Math.abs(before.groundY-after.groundY)<1.0f);
  }
 }
 @Test public void freeRunTurnsBothWaysAndHasActualJumpPhases() {
  boolean right=false,left=false,jump=false,grounded=false;
  for(long t=0;t<FreeRunChoreography.LOOP_MS;t+=100){
   FreeRunChoreography.Pose p=FreeRunChoreography.sample(t,0);
   right|=p.directionX>0.1f;left|=p.directionX< -0.1f;
   jump|=p.hop>10f;grounded|=p.hop==0f;
  }
  assertTrue("route includes rightward runs",right);
  assertTrue("route includes leftward runs",left);
  assertTrue("route includes natural hops",jump);
  assertTrue("character returns to ground between hops",grounded);
 }
 @Test public void spriteFramesAdvanceDuringRestAndFramePacingIsVsyncFriendly() {
  assertEquals(0,PremiumSceneView.spriteCellAt(0,67));
  assertEquals(1,PremiumSceneView.spriteCellAt(67,67));
  assertEquals(0,PremiumSceneView.spriteCellAt(402,67));
  assertTrue(PremiumSceneView.spriteCellAt(6400,67)!=PremiumSceneView.spriteCellAt(6467,67));
  assertTrue(PremiumSceneView.spriteCellAt(7000,67)!=PremiumSceneView.spriteCellAt(7067,67));
  assertEquals(33_333_333L,PremiumSceneView.frameIntervalNanos(2));
  assertEquals(66_666_667L,PremiumSceneView.frameIntervalNanos(1));
  assertEquals(33L,PremiumSceneView.frameDelayMillis(2));
  assertEquals(67L,PremiumSceneView.frameDelayMillis(1));
  assertEquals(0L,PremiumSceneView.frameDelayMillis(0));
  assertEquals(Long.MAX_VALUE,PremiumSceneView.frameIntervalNanos(0));
 }

}
