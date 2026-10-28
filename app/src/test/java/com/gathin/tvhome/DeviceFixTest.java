package com.gathin.tvhome;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.location.*;
import android.os.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.GraphicsMode;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DeviceFixTest {
    void pump(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));}
    Location fix(long now){Location l=new Location("network");l.setLatitude(0);l.setLongitude(0);l.setAccuracy(3000);l.setTime(now);return l;}
    void grant(){Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);}
    @Test public void acceptsApproximateLocationButRejectsStaleInvalidAndInaccurate(){long now=System.currentTimeMillis();Location l=fix(now);assertTrue(DeviceLocation.usable(l,now,0));l.setTime(now-DeviceLocation.MAX_AGE_MS-1);assertFalse(DeviceLocation.usable(l,now,0));l=fix(now);l.setAccuracy(60_000);assertFalse(DeviceLocation.usable(l,now,0));l=fix(now);l.setLatitude(91);assertFalse(DeviceLocation.usable(l,now,0));assertFalse(DeviceLocation.usable(null,now,0));}
    @Test public void androidLocationRejectsPermissionBeforeUsingProviders(){Context c=RuntimeEnvironment.getApplication();Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);AtomicReference<String> error=new AtomicReference<>();DeviceLocation.AndroidSource source=new DeviceLocation.AndroidSource(c);source.request(new DeviceLocation.Result(){public void found(Location l){fail();}public void failed(String s){error.set(s);}});assertNotNull(error.get());assertTrue(error.get().contains("dibenarkan"));source.cancel();}
    @Test public void locationMenuActuallyShowsDetectionAndAndroidSettings(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        c.get().currentWeatherSettings();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d.getListView());assertEquals(5,d.getListView().getCount());assertTrue(d.getListView().getItemAtPosition(0).toString().contains("Detect lokasi"));assertTrue(d.getListView().getItemAtPosition(1).toString().contains("Android"));
    }}
    @Test public void detectionExplainsCoordinateSharingAndRequestsPermission74(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);HourlyWeatherController h=new HourlyWeatherController(c.get());h.offerAutomaticLocation();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(Shadows.shadowOf(d).getMessage().toString().contains("Open-Meteo"));d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();pump();var requested=Shadows.shadowOf(c.get()).getLastRequestedPermission();assertNotNull(requested);assertEquals(74,requested.requestCode);assertTrue(Arrays.asList(requested.requestedPermissions).contains(Manifest.permission.ACCESS_COARSE_LOCATION));assertEquals("auto",c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).getString("location_mode",""));assertTrue(c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).getBoolean("enabled",false));h.destroy();
    }}
    static class FakeLocation implements DeviceLocation.Source {int calls,cancels;DeviceLocation.Result result;Location value;String error;public void request(DeviceLocation.Result r){calls++;result=r;if(value!=null)r.found(value);if(error!=null)r.failed(error);}public void cancel(){cancels++;}}
    @Test public void autoLocationStoresDetectedCoordinatesAndStartsCurrentWeather()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            grant();long now=System.currentTimeMillis();FakeLocation location=new FakeLocation();location.value=fix(now);AtomicInteger fetched=new AtomicInteger();
            c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putString("location_mode","auto").putBoolean("enabled",true).commit();
            HourlyWeatherController h=new HourlyWeatherController(c.get(),new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){return new HourlyWeather(now,new HourlyWeather.Point(now/1000,28,3),Collections.emptyList());}public HourlyWeather fetch(double a,double b,long n){fetched.incrementAndGet();return null;}},Clock.fixed(Instant.ofEpochMilli(now),ZoneOffset.UTC),location);
            try{h.resume();for(int i=0;i<200&&!h.view().getContentDescription().toString().contains("28°C");i++){Thread.sleep(5);pump();}assertTrue(h.configured());assertEquals(1,location.calls);assertEquals("0.0",c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).getString("latitude",""));assertEquals("Lokasi TV",c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).getString("label",""));assertEquals(0,fetched.get());}finally{h.destroy();}
        }
    }
    @Test public void pausedLocationResultIsIgnoredAndListenerCancelled(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        grant();FakeLocation location=new FakeLocation();long now=System.currentTimeMillis();c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putString("location_mode","auto").putBoolean("enabled",true).commit();
        HourlyWeatherController h=new HourlyWeatherController(c.get(),new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){return null;}public HourlyWeather fetch(double a,double b,long n){fail();return null;}},Clock.systemUTC(),location);
        h.resume();assertEquals(1,location.calls);h.pause();assertTrue(location.cancels>0);location.result.found(fix(now));assertFalse(h.configured());h.destroy();
    }}
    @Test public void noProviderErrorIsVisibleWithoutInventedCoordinates(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        grant();FakeLocation location=new FakeLocation();location.error="TV tiada provider lokasi";c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putString("location_mode","auto").putBoolean("enabled",true).commit();
        HourlyWeatherController h=new HourlyWeatherController(c.get(),new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){return null;}public HourlyWeather fetch(double a,double b,long n){fail();return null;}},Clock.systemUTC(),location);
        h.resume();assertFalse(h.configured());assertTrue(h.view().getContentDescription().toString().contains(location.error));h.pause();h.destroy();
    }}
    @Test public void packMappingSupportsAliasesRelativeNamesAndReferences()throws Exception{
        ResolveInfo app=new ResolveInfo();app.activityInfo=new ActivityInfo();app.activityInfo.packageName="test.app";app.activityInfo.name=".Alias";app.activityInfo.targetActivity=".Main";
        Map<String,String> values=new HashMap<>();values.put("test.app/test.app.Main","banner");assertEquals("banner",AppArtwork.mapped(values,app));assertEquals("test.app/test.app.Main",AppArtwork.component("ComponentInfo{test.app/.Main}"));assertEquals("banner",AppArtwork.drawable("@drawable/banner"));assertEquals("@0x7f080001",AppArtwork.drawable("@0x7f080001"));assertEquals("",AppArtwork.drawable("../unsafe"));
    }
    @Test public void brokenPackHasDiagnosticInsteadOfSilentFailure(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences(AppArtwork.PREFS,0).edit().putString("pack","missing.pack").commit();String status=new AppArtwork(c).status();assertTrue(status.contains("missing.pack"));assertTrue(status.contains("tidak dapat dibaca"));}
}
