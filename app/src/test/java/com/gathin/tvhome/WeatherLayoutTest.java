package com.gathin.tvhome;
import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WeatherLayoutTest {
 final DashboardTest helper=new DashboardTest();
 static final Instant NOW=Instant.parse("2026-10-27T02:00:00Z");
 WeatherForecast forecast(String text,long age,String id){return new WeatherForecast(id,NOW.toEpochMilli()-age,Collections.singletonMap(LocalDate.of(2026,10,27),new String[]{text,text,text}));}
 RainController attach(DashboardActivity a,WeatherForecast f)throws Exception{
  a.rain.destroy();RainOptions.prefs(a).edit().remove("request_not_before").remove("retry_at").commit();
  RainController.Source source=new RainController.Source(){public WeatherForecast cached(String id){return f;}public WeatherForecast fetch(String id,long stamp){fail("No network expected");return null;}};
  RainController c=new RainController(a,a.findViewById(R.id.root),source,Clock.fixed(NOW,ZoneOffset.UTC),()->false);a.rain=c;
  c.attachWeather(a.findViewById(R.id.root).findViewWithTag("weather-widget"));c.resume();
  for(int i=0;i<500&&c.isBusy();i++){Thread.sleep(5);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertFalse(c.isBusy());return c;
 }
 void geometry(int w,int h,String name)throws Exception{helper.demoApps();try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  DashboardActivity a=activity.get();View root=helper.render(a,w,h),grid=root.findViewById(R.id.grid),weather=root.findViewWithTag("weather-widget");
  assertTrue("Apps at bottom",grid.getTop()>h*.72);assertTrue("Bottom safe margin",grid.getBottom()<h*.97);
  assertTrue("Weather to right",weather.getLeft()>w*.70);assertTrue(weather.getRight()<=w*.97);
  assertFalse(weather.isFocusable());assertTrue(weather.getBottom()<grid.getTop());helper.capture(root,name);
 }}
 @Test public void geometry1080()throws Exception{geometry(1920,1080,"weather-layout-1080");}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void geometry720()throws Exception{geometry(1280,720,"weather-layout-720");}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void geometry4k()throws Exception{geometry(3840,2160,"weather-layout-4k");}
 @Test public void realControllerChangesAllFourScenesAndCaptures()throws Exception{
  helper.demoApps();try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   DashboardActivity a=activity.get();int i=0;
   for(String text:new String[]{"Tiada hujan","Berawan","Hujan","Ribut petir"}){
    RainController c=attach(a,forecast(text,0,RainOptions.HOME_ID));assertEquals(WeatherWallpapers.asset(c.currentState()),a.currentWallpaperKey());
    View root=helper.render(a,1920,1080);helper.capture(root,"weather-scene-"+(i++));
   }
  }
 }
 @Test public void invalidStaleFutureWrongLocationAndUnknownRestoreManual()throws Exception{
  try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
   DashboardActivity a=activity.get();
   for(WeatherForecast f:Arrays.asList(null,forecast("Hujan",WeatherForecast.FRESH_MILLIS,RainOptions.HOME_ID),forecast("Hujan",-1000,RainOptions.HOME_ID),forecast("Hujan",0,"St502"),forecast("Tidak diketahui",0,RainOptions.HOME_ID))){
    a.weatherWallpaper(WeatherForecast.State.RAIN_EXPECTED);attach(a,f);assertEquals("manual:"+FayyadhWallpapers.DEFAULT,a.currentWallpaperKey());
   }
  }
 }
 @Test public void dailySummaryNeverDrivesScene()throws Exception{
  try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
   String raw="[{\"location\":{\"location_id\":\"Ds548\"},\"date\":\"2026-10-27\",\"summary_forecast\":\"Ribut petir\"}]";
   attach(activity.get(),WeatherForecast.parse(raw,"Ds548",NOW.toEpochMilli()));assertTrue(activity.get().currentWallpaperKey().startsWith("manual:"));
  }
 }
 @Test public void manualSelectionDisablesAutoAndReenableUsesLatestState()throws Exception{
  try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
   DashboardActivity a=activity.get();attach(a,forecast("Hujan",0,RainOptions.HOME_ID));a.wallpapers();
   AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getListView().performItemClick(null,2,2);
   assertFalse(a.automaticWallpaper());assertEquals("manual:"+FayyadhWallpapers.FILES[2],a.currentWallpaperKey());
   a.weatherWallpaper(WeatherForecast.State.THUNDERSTORM_EXPECTED);assertEquals("manual:"+FayyadhWallpapers.FILES[2],a.currentWallpaperKey());
   a.setAutomaticWallpaper(true);assertEquals(WeatherWallpapers.asset(WeatherForecast.State.THUNDERSTORM_EXPECTED),a.currentWallpaperKey());
   assertEquals(FayyadhWallpapers.FILES[2],a.getSharedPreferences("fayyadh-wallpaper",0).getString("selected",""));
  }
 }
 @Test public void unchangedStateReusesBitmapAndPreviewDoesNotInventWeather()throws Exception{
  try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
   DashboardActivity a=activity.get();RainController c=attach(a,forecast("Berawan",0,RainOptions.HOME_ID));
   android.widget.ImageView image=(android.widget.ImageView)((FrameLayout)a.findViewById(R.id.root)).getChildAt(0);Object before=image.getDrawable();
   c.preview(true);assertSame(before,image.getDrawable());assertEquals(WeatherWallpapers.asset(WeatherForecast.State.CLOUDY),a.currentWallpaperKey());
  }
 }
}
