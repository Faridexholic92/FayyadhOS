package com.gathin.tvhome;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import java.time.*;
import java.io.IOException;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
public class LocationResumeRegressionTest {
 HourlyWeatherController.Source source(){return new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){return null;}public HourlyWeather fetch(double a,double b,long n){return null;}};}
 void attemptAfterPermission(boolean priorFix)throws Exception{
  try(var a=Robolectric.buildActivity(DashboardActivity.class).setup()){
   Application ctx=RuntimeEnvironment.getApplication();Shadows.shadowOf(ctx).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
   SharedPreferences p=a.get().getSharedPreferences(HourlyWeatherController.PREFS,0);
   p.edit().putBoolean("enabled",true).putString("location_mode","manual").putBoolean("location_offer_shown",true).putLong("location_retry_at",System.currentTimeMillis()+900_000).commit();
   if(priorFix)p.edit().putString("latitude","5.9").putString("longitude","116.1").putLong("location_fix_at",System.currentTimeMillis()).commit();
   DeviceFixTest.FakeLocation location=new DeviceFixTest.FakeLocation();
   HourlyWeatherController h=new HourlyWeatherController(a.get(),source(),Clock.systemUTC(),location);
   try{
    h.resume();h.offerAutomaticLocation();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
    assertEquals("auto",p.getString("location_mode",""));assertNotNull(Shadows.shadowOf(a.get()).getLastRequestedPermission());
    h.pause();Shadows.shadowOf(ctx).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);h.permissionResult();
    assertEquals("No request while activity is paused",0,location.calls);
    h.resume();assertEquals("Explicit detection must survive permission/resume, cache and retry gate",1,location.calls);
    h.pause();
   }finally{h.destroy();}
  }
 }
 @Test public void permissionResumeDoesNotReuseOldFixInsteadOfDetecting()throws Exception{attemptAfterPermission(true);}
 @Test public void permissionResumeDoesNotSilentlyWaitForOldRetryGate()throws Exception{attemptAfterPermission(false);}
 @Test public void settingsExposeDirectDetectionAndSpeedtest()throws Exception{try(var a=Robolectric.buildActivity(DashboardActivity.class).setup()){
  a.get().settings();AlertDialog settings=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Detect lokasi cuaca",settings.getListView().getItemAtPosition(14));assertEquals("Speedtest · Download",settings.getListView().getItemAtPosition(15));
  settings.getListView().performItemClick(null,14,14);assertTrue(Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage().toString().contains("Open-Meteo"));
  a.get().showSpeedtest();android.app.Dialog d=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(JarvisRepairTest.find(d.getWindow().getDecorView(),"Mula Speedtest"));d.dismiss();
 }}
 @Test public void speedDeadlineErrorExplainsTimeoutInsteadOfGenericEndpoint(){assertTrue(SpeedProbe.error(new IOException("Had masa keseluruhan 120 saat dicapai")).contains("120"));}
}
