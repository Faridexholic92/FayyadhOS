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
 int visibleSpritePixels(Bitmap bitmap,float centerX,float bottom,float height){
  float width=height*192f/320f;
  int left=Math.max(0,(int)Math.floor(centerX-width/2f));
  int right=Math.min(bitmap.getWidth(),(int)Math.ceil(centerX+width/2f));
  int top=Math.max(0,(int)Math.floor(bottom-height));
  int lower=Math.min(bitmap.getHeight(),(int)Math.ceil(bottom));
  int count=0;
  for(int y=top;y<lower;y++)for(int x=left;x<right;x++)if(Color.alpha(bitmap.getPixel(x,y))>0)count++;
  return count;
 }
 @Test public void birthdayUsesCleanBackdropForSingleAnimatedCharacterLayer(){
  assertEquals("premium/world.webp",PremiumTheme.asset(true));
  assertEquals("premium/world.webp",PremiumTheme.asset(false));
 }
 @Test public void datedBirthdayNeverRepeatsNextYear(){assertTrue(PremiumTheme.birthday(PremiumTheme.WORLD,true,LocalDate.of(2026,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.WORLD,true,LocalDate.of(2027,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.WORLD,false,LocalDate.of(2026,10,30)));assertFalse(PremiumTheme.birthday(PremiumTheme.CLASSIC,true,LocalDate.of(2026,10,30)));assertTrue(PremiumTheme.birthday(PremiumTheme.BIRTHDAY,false,LocalDate.of(2026,10,29)));}
 @Test public void packagedArtAndAtlasHaveBoundedDecodedSize()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  for(String asset:new String[]{"premium/world.webp","premium/birthday.webp","premium/heroes-run.webp","premium/heroes-pose.webp"})try(InputStream in=c.get().getAssets().open(asset)){Bitmap b=BitmapFactory.decodeStream(in);assertNotNull(asset,b);assertTrue(b.getWidth()<=1920);assertTrue(b.getHeight()<=1080);if(asset.contains("heroes")){assertTrue(b.hasAlpha());if(asset.contains("-run")){assertEquals(1152,b.getWidth());assertEquals(640,b.getHeight());}else{assertEquals(631,b.getWidth());assertEquals(483,b.getHeight());}}b.recycle();}
 }}
 @Test public void motionStopsForOverlayPauseScreenAudioAndDestroy(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  PremiumSceneView s=scene(c.get());s.characters(false);assertTrue(s.hasFrame());s.obscured(true);assertFalse(s.hasFrame());s.obscured(false);assertTrue(s.hasFrame());s.screen(false);assertFalse(s.hasFrame());s.screen(true);assertTrue(s.hasFrame());s.audioBusy(true);assertFalse(s.hasFrame());s.audioBusy(false);assertTrue(s.hasFrame());s.hostActive(false);assertFalse(s.hasFrame());s.hostActive(true);assertTrue(s.hasFrame());s.destroy();assertFalse(s.hasFrame());assertFalse(s.canAnimate());
 }}
 @Test public void offLiteAndSystemReducedMotionAreHonoured(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  PremiumSceneView s=scene(c.get());s.characters(false);s.configure(true,false,0);assertFalse(s.hasFrame());s.configure(true,false,1);assertTrue(s.hasFrame());Settings.Global.putFloat(c.get().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,0);s.configure(true,false,2);assertFalse(s.hasFrame());Settings.Global.putFloat(c.get().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1);RainOptions.prefs(c.get()).edit().putBoolean("moving",false).commit();s.configure(true,false,2);assertFalse(s.hasFrame());
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
  DashboardActivity a=c.get();PremiumTheme.prefs(a).edit().putString("characters",PremiumTheme.RUN).commit();a.weatherWallpaper(WeatherForecast.State.CLEAR);View root=helper.render(a,1920,1080);View before=root.findFocus();assertNotNull(before);a.themeSettings();assertFalse(scene(a).hasFrame());AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(6,dialog.getListView().getCount());dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("Scene resumes after dialog dismissal",scene(a).hasFrame());assertSame(before,root.findFocus());
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
   WeatherWidget weather=root.findViewWithTag("weather-widget");weather.update(KinarutWeatherTest.fixture(0),"Ds548","Kinarut",KinarutWeatherTest.NOW,true);
   a.weatherWallpaper(WeatherForecast.State.RAIN_EXPECTED);field(scene(a),"elapsed",2500L);
   ((TextView)root.findViewWithTag("greeting")).setText(MotivationQuotes.at(0));
   TextView mark=new TextView(a);mark.setText("UI SIMULATION · fixture apps / JAKIM schedule / MET weather · not a physical TV capture");mark.setTextColor(0xffd1dbe6);mark.setTextSize(9f*w/1920f);mark.setGravity(Gravity.CENTER);mark.setShadowLayer(2,0,1,Color.BLACK);
   FrameLayout.LayoutParams markParams=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);markParams.bottomMargin=Math.round(8f*w/1920f);((FrameLayout)root).addView(mark,markParams);
   root=helper.render(a,w,h);View grid=root.findViewById(R.id.grid);assertTrue(grid.getTop()>h*.78);assertTrue(grid.getBottom()<h*.96);assertTrue(prayer.getBottom()<h*.34);
   View hourlyView=root.findViewWithTag("hourly-weather");assertEquals("Open-Meteo row stays hidden until configured",View.GONE,hourlyView.getVisibility());
   ViewGroup hourlyRow=(ViewGroup)((ViewGroup)hourlyView).getChildAt(0);
   if(hourlyView.getVisibility()==View.VISIBLE)for(int i=0;i<3;i++){ViewGroup column=(ViewGroup)hourlyRow.getChildAt(i);TextView title=(TextView)column.getChildAt(0);assertTrue("Compact weather heading fits",title.getPaint().measureText(title.getText().toString())<=column.getWidth());}

   assertEquals(Color.TRANSPARENT,((android.graphics.drawable.GradientDrawable)prayer.getBackground()).getColor().getDefaultColor());
   if(name.contains("glass")){
    RainOptions.prefs(a).edit().putInt("intensity",1).putInt("density",1).commit();a.rain.preview(true);
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(2100));
    weather.update(KinarutWeatherTest.fixture(0),"Ds548","Kinarut",KinarutWeatherTest.NOW,true);
    root=helper.render(a,w,h);
   }
   helper.capture(root,name);
   if(frames&&scene(a).canAnimate()){for(int i=0;i<90;i++){field(scene(a),"elapsed",i*100L);helper.capture(root,String.format(java.util.Locale.ROOT,"premium-motion/frame-%03d",i));}}
  }
 }
 @Test public void native1080()throws Exception{preview(1920,1080,"premium-home-1080",false,false);}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void native720WithMotionFrames()throws Exception{preview(1280,720,"premium-home-720",false,true);}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void native4k()throws Exception{preview(3840,2160,"premium-home-4k",false,false);}
 @Test public void refractiveGlass1080()throws Exception{preview(1920,1080,"premium-glass-1080",false,false);}
 @Test public void birthday1080()throws Exception{preview(1920,1080,"premium-birthday-1080",true,false);}

 @Test public void birthdaySceneContinuesAndDrawsSpritesAfterConfettiWindow()throws Exception {
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   PremiumSceneView s=scene(c.get());
   s.characters(false); // Free-run sprite mode; hero pose is covered separately.
   s.configure(true,true,2);
   s.hostActive(true); // Match DashboardActivity.onResume() in this isolated renderer test.
   field(s,"partyStarted",SystemClock.uptimeMillis()-11_000L);
   assertTrue("Confetti expiry must not stop the Birthday character loop",s.canAnimate());
   field(s,"elapsed",1_000L);
   s.layout(0,0,960,540);
   Bitmap frame=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
   try{
    s.draw(new Canvas(frame));
    FreeRunChoreography.Pose papaPose=FreeRunChoreography.sample(1_000L,0);
    FreeRunChoreography.Pose boyPose=FreeRunChoreography.sample(1_000L,1);
    float papaBottom=papaPose.groundY-papaPose.hop+FreeRunChoreography.bobAt(1_000L,0);
    float boyBottom=boyPose.groundY-boyPose.hop+FreeRunChoreography.bobAt(1_000L,1);
    int papaPixels=visibleSpritePixels(frame,papaPose.x,papaBottom,206f*papaPose.scale);
    int boyPixels=visibleSpritePixels(frame,boyPose.x,boyBottom,156f*boyPose.scale);
    assertTrue("Birthday theme must draw PapaZola at his free-run position after confetti expires",papaPixels>100);
    assertTrue("Birthday theme must draw BoBoiBoy at his free-run position after confetti expires",boyPixels>100);
   }finally{frame.recycle();}
  }
 }
 @Test public void heroPoseIsDefaultAndDrawsBothCharactersAtMockupPlaces()throws Exception{
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   PremiumSceneView s=scene(c.get());assertTrue("Static hero pose is the default character style",s.heroPose());assertFalse("Static Home must not schedule scene animation",s.hasFrame());
   s.configure(true,false,2);s.layout(0,0,960,540);Bitmap frame=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
   try{s.draw(new Canvas(frame));
    assertTrue(frame.getPixel((int)PremiumSceneView.PAPA_PLACE[0],(int)(PremiumSceneView.PAPA_PLACE[1]-PremiumSceneView.PAPA_PLACE[2]*.55f))>>>24>200);
    assertTrue("Supplied Halilintar is drawn",visibleSpritePixels(frame,PremiumSceneView.BOY_PLACE[0],PremiumSceneView.BOY_PLACE[1],PremiumSceneView.BOY_PLACE[2])>100);
    assertEquals("left hero column stays clear of characters",0,frame.getPixel(120,300)>>>24);
   }finally{frame.recycle();}
   PremiumTheme.prefs(c.get()).edit().putString("characters",PremiumTheme.RUN).commit();c.get().weatherWallpaper(WeatherForecast.State.CLEAR);assertFalse(s.heroPose());
  }
 }
 @Test public void heroPoseIsStaticAtEveryTimestamp(){
  for(long t=0;t<4000;t+=40){float a=PremiumSceneView.poseBob(t,0),b=PremiumSceneView.poseBob(t,1);assertEquals(0f,a,0f);assertEquals(0f,b,0f);}
  assertEquals(PremiumSceneView.poseBob(1234,0),PremiumSceneView.poseBob(1234,0),0f);
 }
 @Test public void motivationRotatesThroughOfflineQuotes(){
  assertEquals(MotivationQuotes.LINES[0],MotivationQuotes.at(0));assertEquals(MotivationQuotes.LINES[1],MotivationQuotes.at(MotivationQuotes.ROTATE_MS));
  assertEquals(MotivationQuotes.LINES[0],MotivationQuotes.at(MotivationQuotes.ROTATE_MS*MotivationQuotes.LINES.length));
  for(String line:MotivationQuotes.LINES)assertTrue(line.length()<=60);
 }
 @Test public void heroBrightnessTracksWeatherBackdrop()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  DashboardActivity a=c.get();PremiumSceneView s=scene(a);s.layout(0,0,960,540);Bitmap normal=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888),storm=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
  try{a.weatherWallpaper(WeatherForecast.State.RAIN_EXPECTED);s.draw(new Canvas(normal));a.weatherWallpaper(WeatherForecast.State.THUNDERSTORM_EXPECTED);s.draw(new Canvas(storm));int n=normal.getPixel(400,300),t=storm.getPixel(400,300);assertTrue(Color.alpha(n)>200);assertEquals(Color.alpha(n),Color.alpha(t));assertTrue(Color.red(t)<Color.red(n));}finally{normal.recycle();storm.recycle();}
 }}
 @Test public void referenceLogoAndBlendedAtlasArePackaged()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  ImageView logo=c.get().findViewById(R.id.root).findViewWithTag("fayyadhos-logo");assertNotNull(logo);assertNotNull(logo.getDrawable());
  for(String file:new String[]{"premium/fayyadhos-logo.webp","premium/heroes-blended.webp"})try(InputStream in=c.get().getAssets().open(file)){Bitmap b=BitmapFactory.decodeStream(in);assertNotNull(b);assertTrue(b.hasAlpha());b.recycle();}
 }}
 @Test public void staticHomeKeepsRainMovementIndependent()throws Exception{
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   DashboardActivity a=c.get();PremiumSceneView s=scene(a);assertFalse(s.canAnimate());
   s.configure(true,true,2);s.layout(0,0,960,540);
   Bitmap first=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888),later=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
   try{field(s,"elapsed",0L);s.draw(new Canvas(first));field(s,"elapsed",5000L);s.draw(new Canvas(later));assertTrue("Default scene stays pixel-identical over time",first.sameAs(later));}finally{first.recycle();later.recycle();}
   View wallpaper=a.findViewById(R.id.root).findViewWithTag("premium-scene");assertNotNull(wallpaper);
   RainGlassView rain=new RainGlassView(a);rain.layout(0,0,960,540);rain.configure(new RainOptions(RainOptions.prefs(a)));rain.setHostActive(true);rain.setRain(true);
   try{assertTrue("Flowing rain remains enabled with a static Home",rain.activeMovingCount()>0);assertTrue(rain.hasScheduledFrame());}finally{rain.destroy();}
  }
 }
 @Test public void freeRunPathsStayClearOfGreetingDockAndEachOther() {
  for(long t=0;t<FreeRunChoreography.LOOP_MS;t+=100){
   FreeRunChoreography.Pose papa=FreeRunChoreography.sample(t,0);
   FreeRunChoreography.Pose boy=FreeRunChoreography.sample(t,1);
   assertTrue("PapaZola stays clear of the left greeting column",papa.x>=360f&&papa.x<=570f);
   assertTrue("BoBoiBoy stays inside the right action lane",boy.x>=695f&&boy.x<=890f);
   assertTrue("two character lanes do not overlap",boy.x-papa.x>=120f);
   for(FreeRunChoreography.Pose p:new FreeRunChoreography.Pose[]{papa,boy}){
    assertTrue("ground stays clear of top widgets and app dock",p.groundY>=340f&&p.groundY<=425f);
    assertTrue("depth scale stays bounded",p.scale>=0.78f&&p.scale<=0.99f);
    assertTrue("jump offset stays bounded",p.hop>=0f&&p.hop<=18.1f);
    assertTrue("direction is finite",Float.isFinite(p.directionX)&&Float.isFinite(p.directionY));
    assertTrue("body lean is restrained",Math.abs(p.leanDegrees)<=4.2f);
   }
  }
 }
 @Test public void routesStartAtTheirFirstKnotAndWrapWithoutTeleporting() {
  FreeRunChoreography.Pose papaStart=FreeRunChoreography.sample(0,0);
  FreeRunChoreography.Pose boyStart=FreeRunChoreography.sample(-2_150L,1);
  assertEquals(370f,papaStart.x,0.5f);
  assertEquals(880f,boyStart.x,0.5f);
  for(int character=0;character<2;character++){
   long offset=character==0?0L:2_150L;
   FreeRunChoreography.Pose before=FreeRunChoreography.sample(
           FreeRunChoreography.LOOP_MS-1-offset,character);
   FreeRunChoreography.Pose after=FreeRunChoreography.sample(1-offset,character);
   assertTrue("loop seam is position-continuous",Math.abs(before.x-after.x)<1.0f);
   assertTrue("loop seam is depth-continuous",Math.abs(before.groundY-after.groundY)<1.0f);
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
 @Test public void bothFreeRunRoutesCurveAndIncludeNaturalJumpPhases() {
  for(int character=0;character<2;character++){
   boolean right=false,left=false,up=false,down=false,jump=false,grounded=false;
   for(long t=0;t<FreeRunChoreography.LOOP_MS;t+=100){
    FreeRunChoreography.Pose p=FreeRunChoreography.sample(t,character);
    right|=p.directionX>0.1f;left|=p.directionX< -0.1f;
    up|=p.directionY< -0.12f;down|=p.directionY>0.12f;
    jump|=p.hop>10f;grounded|=p.hop==0f;
   }
   assertTrue("route includes rightward runs",right);
   assertTrue("route includes leftward runs",left);
   assertTrue("route has a depth change toward the top",up);
   assertTrue("route has a depth change toward the bottom",down);
   assertTrue("route includes natural hops",jump);
   assertTrue("character returns to ground between hops",grounded);
  }
 }
 @Test public void bothCharactersFaceTheirDirectionOfTravel() {
  assertTrue(FreeRunChoreography.mirrorForDirection(0,-1f));
  assertFalse(FreeRunChoreography.mirrorForDirection(0,1f));
  assertTrue(FreeRunChoreography.mirrorForDirection(1,-1f));
  assertFalse(FreeRunChoreography.mirrorForDirection(1,1f));
 }
 @Test public void characterRunCyclesAreDesynchronised() {
  assertNotEquals(FreeRunChoreography.spriteCellAt(0,67,0),
          FreeRunChoreography.spriteCellAt(0,67,1));
  assertNotEquals(FreeRunChoreography.spriteCellAt(67,67,0),
          FreeRunChoreography.spriteCellAt(67,67,1));
  assertNotEquals(FreeRunChoreography.spriteCellAt(0,67,1),
          FreeRunChoreography.spriteCellAt(60,67,1));
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
