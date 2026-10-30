package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import androidx.recyclerview.widget.RecyclerView;
import org.json.*;
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
public class FeaturePackTest {
    static final long NOW=Instant.parse("2026-10-28T01:15:00Z").toEpochMilli();
    static String weather(long now)throws Exception{
        long hour=now/1000/3600*3600;
        JSONObject json=new JSONObject().put("current",new JSONObject().put("time",now/1000).put("temperature_2m",28.5).put("weather_code",3));
        JSONObject units=new JSONObject().put("time","unixtime").put("temperature_2m","°C");json.put("current_units",units).put("hourly_units",new JSONObject(units.toString()));
        JSONArray time=new JSONArray(),temp=new JSONArray(),code=new JSONArray();for(int i=0;i<48;i++){time.put(hour+i*3600);temp.put(20+i%10);code.put(i==3?95:0);}
        return json.put("hourly",new JSONObject().put("time",time).put("temperature_2m",temp).put("weather_code",code)).toString();
    }
    void idle(long millis){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));}
    Bitmap solid(int color){Bitmap b=Bitmap.createBitmap(16,9,Bitmap.Config.ARGB_8888);b.eraseColor(color);return b;}
    WallpaperDissolveView image(){WallpaperDissolveView v=new WallpaperDissolveView(RuntimeEnvironment.getApplication());v.measure(View.MeasureSpec.makeMeasureSpec(160,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(90,View.MeasureSpec.EXACTLY));v.layout(0,0,160,90);v.hostActive(true);return v;}
    @Test public void virtualRunwayMapsBothDirections(){assertEquals(6,LoopCarousel.logical(LoopCarousel.anchor(0,7)-1,7));assertEquals(0,LoopCarousel.logical(LoopCarousel.anchor(6,7)+1,7));assertEquals(1407,LoopCarousel.count(7,false));assertEquals(7,LoopCarousel.count(7,true));assertEquals(0,LoopCarousel.count(0,false));assertEquals(1,LoopCarousel.count(1,false));}
    @Test public void carouselWrapsFromFirstToLastAndLaunchesRealLastApp(){DashboardTest f=new DashboardTest();f.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
        View root=f.render(c.get(),1920,1080);RecyclerView grid=root.findViewById(R.id.grid);int first=LoopCarousel.anchor(0,7);grid.findViewHolderForAdapterPosition(first).itemView.requestFocus();
        root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_LEFT));idle(600);f.render(c.get(),1920,1080);
        View focus=root.findFocus();int position=grid.getChildAdapterPosition(focus);assertEquals(6,LoopCarousel.logical(position,7));
        root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER));root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER));Intent opened=Shadows.shadowOf(c.get()).getNextStartedActivity();assertEquals(new DashboardApps(c.get()).visible().get(6).activityInfo.packageName,opened.getComponent().getPackageName());
    }}
    @Test public void carouselCenterHasZoomAndEdgeScale(){DashboardTest f=new DashboardTest();f.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
        View root=f.render(c.get(),1920,1080);LoopCarousel grid=root.findViewById(R.id.grid);grid.effects();View center=grid.findViewHolderForAdapterPosition(LoopCarousel.anchor(0,7)).itemView;
        assertEquals(1.08f,center.getScaleX(),.04f);assertTrue(grid.getChildAt(0).getScaleX()<center.getScaleX());assertTrue(Math.abs((center.getLeft()+center.getRight())/2-grid.getWidth()/2)<24);
    }}
    @Test public void wallpaperDissolvesWithoutBlackFrame(){WallpaperDissolveView v=image();v.request("clear",()->solid(Color.RED));assertFalse(v.animating());v.request("rain",()->solid(Color.BLUE));assertTrue(v.animating());idle(400);
        Bitmap out=Bitmap.createBitmap(160,90,Bitmap.Config.ARGB_8888);v.draw(new Canvas(out));int pixel=out.getPixel(80,45);assertTrue(Color.red(pixel)>60);assertTrue(Color.blue(pixel)>60);idle(450);assertFalse(v.animating());v.destroy();}
    @Test public void wallpaperQueueOnlyDecodesLatestState(){WallpaperDissolveView v=image();AtomicInteger dropped=new AtomicInteger(),last=new AtomicInteger();v.request("a",()->solid(Color.RED));v.request("b",()->solid(Color.BLUE));v.request("c",()->{dropped.incrementAndGet();return solid(Color.GREEN);});v.request("d",()->{last.incrementAndGet();return solid(Color.WHITE);});assertEquals(0,last.get());idle(850);assertEquals(0,dropped.get());assertEquals(1,last.get());idle(850);assertEquals("d",v.desiredKey());assertFalse(v.animating());v.destroy();}
    @Test public void sameWallpaperNeverDecodesAgain(){WallpaperDissolveView v=image();v.request("a",()->solid(Color.RED));v.request("a",()->{fail("unchanged key decoded");return null;});assertFalse(v.animating());v.destroy();}
    @Test public void reducedMotionDisablesDissolve(){Settings.Global.putFloat(RuntimeEnvironment.getApplication().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,0);WallpaperDissolveView v=image();v.request("a",()->solid(Color.RED));v.request("b",()->solid(Color.BLUE));assertFalse(v.animating());v.destroy();}
    @Test public void pauseDrainsPendingWithoutAnimation(){WallpaperDissolveView v=image();v.request("a",()->solid(Color.RED));v.request("b",()->solid(Color.BLUE));AtomicInteger decoded=new AtomicInteger();v.request("c",()->{decoded.incrementAndGet();return solid(Color.WHITE);});v.hostActive(false);assertEquals(1,decoded.get());assertFalse(v.animating());v.destroy();}
    @Test public void destroyCancelsQueuedDecodes(){WallpaperDissolveView v=image();v.request("a",()->solid(Color.RED));v.request("b",()->solid(Color.BLUE));v.request("c",()->{fail("decode after destroy");return null;});v.destroy();idle(2000);assertFalse(v.animating());}
    @Test public void parsesCurrentAndThreeSixHoursWithMYTDayRollover()throws Exception{long now=Instant.parse("2026-10-28T14:45:00Z").toEpochMilli();HourlyWeather w=HourlyWeather.parse(weather(now),now);Instant instant=Instant.ofEpochMilli(now);assertEquals(28.5,w.current(instant).temperature,0);assertEquals(95,w.after(instant,3).code);assertEquals(1,w.after(instant,3).time==instant.atZone(WeatherForecast.ZONE).toLocalDate().plusDays(1).atTime(1,0).atZone(WeatherForecast.ZONE).toEpochSecond()?1:0);assertNotNull(w.after(instant,6));}
    @Test public void staleAndFutureCachesAreNotDisplayed()throws Exception{HourlyWeather w=HourlyWeather.parse(weather(NOW),NOW);assertFalse(w.fresh(Instant.ofEpochMilli(NOW-1)));assertFalse(w.fresh(Instant.ofEpochMilli(NOW+HourlyWeather.FRESH_MS)));assertNull(w.current(Instant.ofEpochMilli(NOW+HourlyWeather.FRESH_MS)));}
    @Test public void rejectsNullOrNonNumericTemperature()throws Exception{JSONObject json=new JSONObject(weather(NOW));json.getJSONObject("current").put("temperature_2m",JSONObject.NULL);try{HourlyWeather.parse(json.toString(),NOW);fail();}catch(JSONException expected){}json=new JSONObject(weather(NOW));json.getJSONObject("hourly").getJSONArray("temperature_2m").put(3,"28");try{HourlyWeather.parse(json.toString(),NOW);fail();}catch(JSONException expected){}}
    @Test public void rejectsBadUnitsAndIncompleteForecast()throws Exception{JSONObject json=new JSONObject(weather(NOW));json.getJSONObject("current_units").put("temperature_2m","°F");try{HourlyWeather.parse(json.toString(),NOW);fail();}catch(JSONException expected){}json=new JSONObject(weather(NOW));json.getJSONObject("hourly").put("time",new JSONArray().put(1));try{HourlyWeather.parse(json.toString(),NOW);fail();}catch(JSONException expected){}}
    @Test public void rejectsOutOfOrderHours()throws Exception{JSONObject json=new JSONObject(weather(NOW));json.getJSONObject("hourly").getJSONArray("time").put(3,1);try{HourlyWeather.parse(json.toString(),NOW);fail();}catch(JSONException expected){}}
    @Test public void coordinatesAreValidatedAndNotBundled(){assertTrue(HourlyWeatherRepository.validCoordinates(0,0));assertFalse(HourlyWeatherRepository.validCoordinates(91,0));assertFalse(HourlyWeatherRepository.validCoordinates(0,181));assertFalse(HourlyWeatherRepository.validCoordinates(Double.NaN,0));String endpoint=HourlyWeatherRepository.endpoint(0,0);assertTrue(endpoint.startsWith("https://api.open-meteo.com/"));assertTrue(endpoint.contains("timeformat=unixtime"));assertFalse(endpoint.contains("apikey"));}
    @Test public void rejectsOversizedProviderResponse()throws Exception{try{HourlyWeatherRepository.read(new java.io.ByteArrayInputStream(new byte[256*1024+1]),0);fail();}catch(java.io.IOException expected){}}
    @Test public void unconfiguredHourlyNeverRequestsNetwork(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        AtomicInteger requests=new AtomicInteger();HourlyWeatherController h=new HourlyWeatherController(c.get(),new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){requests.incrementAndGet();return null;}public HourlyWeather fetch(double a,double b,long now){requests.incrementAndGet();return null;}},Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));h.resume();idle(1000);assertEquals(0,requests.get());assertFalse(h.configured());h.destroy();
    }}
    @Test public void freshCachedHourlySurvivesOfflineAndIsLabelled()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        c.get().getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putBoolean("enabled",true).putString("latitude","0").putString("longitude","0").commit();HourlyWeather cached=HourlyWeather.parse(weather(NOW),NOW);AtomicInteger fetches=new AtomicInteger();
        HourlyWeatherController h=new HourlyWeatherController(c.get(),new HourlyWeatherController.Source(){public HourlyWeather cached(double a,double b){return cached;}public HourlyWeather fetch(double a,double b,long now)throws Exception{fetches.incrementAndGet();throw new java.io.IOException("offline");}},Clock.fixed(Instant.ofEpochMilli(NOW+31*60*1000L),ZoneOffset.UTC));h.resume();for(int i=0;i<200&&!h.view().getContentDescription().toString().contains("cache");i++){Thread.sleep(5);idle(1);}assertEquals(1,fetches.get());assertTrue(h.view().getContentDescription().toString().contains("cache"));assertTrue(h.view().getContentDescription().toString().contains("+3j"));h.pause();h.destroy();
    }}
    @Test public void noDimWithoutOverlayPermission(){org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false);Context c=RuntimeEnvironment.getApplication();assertFalse(DimService.enable(c));assertFalse(DimService.enabled(c));}
    @Test public void dimIsNonInteractiveWithOffNotification(){org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences(DimService.PREFS,0).edit().putBoolean("enabled",true).commit();var controller=Robolectric.buildService(DimService.class).create();try{
        DimService service=controller.get();assertEquals(Service.START_STICKY,service.onStartCommand(new Intent(),0,1));java.lang.reflect.Field field=DimService.class.getDeclaredField("shade");field.setAccessible(true);View shade=(View)field.get(service);assertNotNull(shade);WindowManager.LayoutParams p=(WindowManager.LayoutParams)shade.getLayoutParams();assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,p.type);assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);assertEquals(89,Color.alpha(DimService.COLOR));Notification note=Shadows.shadowOf(c.getSystemService(NotificationManager.class)).getNotification(DimService.ID);assertNotNull(note);assertEquals("OFF",note.actions[0].title);service.onStartCommand(new Intent().setAction(DimService.STOP),0,2);assertFalse(DimService.enabled(c));
    }catch(Exception e){throw new AssertionError(e);}finally{controller.destroy();}}
    @Test public void settingsKeepOldIndicesAndAppendPremiumEntry(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){c.get().settings();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertEquals(12,dialog.getListView().getCount());assertEquals("PapaZola · Theme & motion",dialog.getListView().getItemAtPosition(11));assertEquals("Azan · Lima waktu",dialog.getListView().getItemAtPosition(8));assertTrue(dialog.getListView().getItemAtPosition(9).toString().contains("+3j"));assertEquals("Dim 35%: OFF",dialog.getListView().getItemAtPosition(10));}}
    @Test public void appNavigationDoesNotChangeWeatherWallpaper(){DashboardTest f=new DashboardTest();f.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){View root=f.render(c.get(),1920,1080);String before=c.get().currentWallpaperKey();root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));idle(600);assertEquals(before,c.get().currentWallpaperKey());}}
    private void preview(int width,int height,String name)throws Exception{
        DashboardTest f=new DashboardTest();f.demoApps();f.seedApiCache();
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
            DashboardActivity a=c.get();View root=f.render(a,width,height);
            a.getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putBoolean("enabled",true).putString("latitude","0").putString("longitude","0").commit();
            HourlyWeather fixture=HourlyWeather.parse(weather(NOW),NOW);
            HourlyWeatherController h=new HourlyWeatherController(a,new HourlyWeatherController.Source(){public HourlyWeather cached(double x,double y){return fixture;}public HourlyWeather fetch(double x,double y,long n){fail("Preview must not fetch live data");return null;}},Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));
            View old=root.findViewWithTag("hourly-weather");android.view.ViewGroup parent=(android.view.ViewGroup)old.getParent();android.view.ViewGroup.LayoutParams params=old.getLayoutParams();parent.removeView(old);parent.addView(h.view(),params);h.resume();
            for(int i=0;i<200&&!h.view().getContentDescription().toString().contains("cache");i++){Thread.sleep(5);idle(1);}assertTrue(h.view().getContentDescription().toString().contains("cache"));
            android.widget.TextView label=GlassUi.text(a,"SIMULATION · test app icons / weather / prayer fixtures · NOT a TV capture",11,Color.WHITE);label.setBackgroundColor(0xb0000000);label.setPadding(a.d(8),a.d(3),a.d(8),a.d(3));
            android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);((android.widget.FrameLayout)root).addView(label,lp);
            f.render(a,width,height);View grid=root.findViewById(R.id.grid);assertTrue(h.view().getBottom()<grid.getTop());assertTrue(h.view().getRight()<=width);f.capture(root,name);h.destroy();
        }
    }
    @Test public void preview1080()throws Exception{preview(1920,1080,"feature-pack-1080");}
    @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void preview720()throws Exception{preview(1280,720,"feature-pack-720");}
    @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void preview4k()throws Exception{preview(3840,2160,"feature-pack-4k");}

    @Test public void parsesRecordedLiveProviderShape()throws Exception{
        try(java.io.InputStream in=getClass().getResourceAsStream("/open-meteo-0-0-live-shape.json")){
            String raw=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            long stamp=new JSONObject(raw).getJSONObject("current").getLong("time")*1000;
            HourlyWeather weather=HourlyWeather.parse(raw,stamp);assertNotNull(weather.current(Instant.ofEpochMilli(stamp)));assertNotNull(weather.after(Instant.ofEpochMilli(stamp),6));
        }
    }

}
