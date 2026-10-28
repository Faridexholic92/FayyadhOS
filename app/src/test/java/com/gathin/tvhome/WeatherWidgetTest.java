package com.gathin.tvhome;

import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WeatherWidgetTest {
    static final Instant NOW=Instant.parse("2026-10-25T01:15:00Z");
    final DashboardTest helper=new DashboardTest();
    static String raw()throws Exception{
        try(var in=WeatherWidgetTest.class.getResourceAsStream("/met-sabah-2026-10-25.json")){
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    static WeatherForecast fixture(long age)throws Exception{return WeatherForecast.parse(raw(),"St502",NOW.toEpochMilli()-age);}
    TextView heading(WeatherWidget widget){return (TextView)((LinearLayout)widget.getChildAt(1)).getChildAt(0);}
    TextView condition(WeatherWidget widget){return (TextView)((LinearLayout)widget.getChildAt(1)).getChildAt(1);}
    WeatherWidget widget(View root){return root.findViewWithTag("weather-widget");}
    Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    void transparent(View v){assertNull("Weather has a background: "+v,v.getBackground());assertNull(v.getForeground());assertEquals(0f,v.getElevation(),0);assertFalse(v.isFocusable());assertFalse(v.isClickable());if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)transparent(((ViewGroup)v).getChildAt(i));}
    void layout(int w,int h,String name)throws Exception{
        helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
            View root=helper.render(c.get(),w,h);WeatherWidget widget=widget(root);widget.update(fixture(0),"St502","Sabah",NOW,false);helper.render(c.get(),w,h);
            transparent(widget);assertEquals("Sabah · 23–34°C",heading(widget).getText().toString());
            assertTrue(condition(widget).getText().toString().startsWith("Ramalan · Ribut petir"));
            Rect area=bounds(widget);assertTrue(area.right<=w);assertTrue(area.top>=0);assertTrue(area.bottom<h);
            for(View other:new View[]{root.findViewById(R.id.btnWifi),root.findViewById(R.id.btnSettings),root.findViewWithTag("greeting")})assertFalse("Overlap with "+other,Rect.intersects(area,bounds(other)));
            // Center clock's parent is the second header child; check both clock and date.
            ViewGroup body=(ViewGroup)((ViewGroup)root).getChildAt(1);ViewGroup header=(ViewGroup)body.getChildAt(0);
            ViewGroup time=(ViewGroup)header.getChildAt(1);assertFalse(Rect.intersects(area,bounds(time)));
            LinearLayout lines=(LinearLayout)widget.getChildAt(1);assertTrue(lines.getHeight()<=widget.getHeight());
            for(int i=0;i<lines.getChildCount();i++){TextView text=(TextView)lines.getChildAt(i);assertEquals(1,text.getLineCount());assertTrue(text.getBottom()<=widget.getHeight());}
            helper.capture(root,name);
            // A blank corner of the widget retains exactly the wallpaper pixels beneath it.
            Bitmap visible=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888),hidden=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            root.draw(new Canvas(visible));widget.setVisibility(View.INVISIBLE);root.draw(new Canvas(hidden));
            assertEquals(hidden.getPixel(area.right-1,area.top),visible.getPixel(area.right-1,area.top));visible.recycle();hidden.recycle();
        }
    }
    @Test public void transparent1080AndNoOverlaps()throws Exception{layout(1920,1080,"weather-1080");}
    @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void transparent720AndNoOverlaps()throws Exception{layout(1280,720,"weather-720");}
    @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void transparent4kAndNoOverlaps()throws Exception{layout(3840,2160,"weather-4k");}
    @Test public void liveResponseRangeIsForecastNotCurrent()throws Exception{
        WeatherForecast forecast=fixture(0);assertEquals("23–34°C",forecast.temperatureRange(NOW));
        assertEquals("Ribut petir di beberapa tempat",forecast.description(NOW));assertEquals(WeatherForecast.State.THUNDERSTORM_EXPECTED,forecast.state(NOW));
        assertFalse(MetWeatherRepository.endpoint("St502").contains("include="));
    }
    @Test public void missingNullInvalidTemperaturesDoNotInventValues()throws Exception{
        for(Object low:new Object[]{JSONObject.NULL,"23",100,30,23.5}){
            JSONArray rows=new JSONArray(raw());rows.getJSONObject(0).put("min_temp",low).put("max_temp",29);
            WeatherForecast forecast=WeatherForecast.parse(rows.toString(),"St502",NOW.toEpochMilli());assertEquals("",forecast.temperatureRange(NOW));
        }
        JSONArray rows=new JSONArray(raw());rows.getJSONObject(0).remove("min_temp");assertEquals("",WeatherForecast.parse(rows.toString(),"St502",NOW.toEpochMilli()).temperatureRange(NOW));
        // Existing rain caches without temperature fields are still accepted.
        assertEquals("",WeatherForecastTest.data("Hujan",0).temperatureRange(WeatherForecastTest.NOW));
        assertEquals("",fixture(WeatherForecast.FRESH_MILLIS).temperatureRange(NOW));
        assertEquals("",fixture(-1).temperatureRange(NOW));
    }
    @Test public void missingExpiredAndWrongLocationHideUnsupportedData()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            WeatherWidget widget=widget(c.get().findViewById(R.id.root));
            for(WeatherForecast forecast:new WeatherForecast[]{null,fixture(WeatherForecast.FRESH_MILLIS),fixture(-1)}){
                widget.update(forecast,"St502","Sabah",NOW,true);assertEquals("Sabah",heading(widget).getText().toString());assertEquals("Ramalan tidak tersedia",condition(widget).getText().toString());
            }
            widget.update(fixture(0),"Ds554","Kota Kinabalu",NOW,false);assertEquals("Kota Kinabalu",heading(widget).getText().toString());assertEquals("Ramalan tidak tersedia",condition(widget).getText().toString());
        }
    }
    static final class Source implements RainController.Source {
        WeatherForecast cache;Exception failure;AtomicInteger calls=new AtomicInteger();String lastId;
        public WeatherForecast cached(String id){lastId=id;return cache;}
        public WeatherForecast fetch(String id,long stamp)throws Exception{lastId=id;calls.incrementAndGet();if(failure!=null)throw failure;return WeatherForecast.parse(KinarutWeatherTest.raw(),"Ds548",stamp);}
    }
    RainController attach(DashboardActivity a,Source source,boolean online)throws Exception{
        a.rain.destroy();
        // Each injected scenario is independent; the production cross-resume cooldown stays intact.
        RainOptions.prefs(a).edit().remove("request_not_before").remove("retry_at").commit();
        RainController r=new RainController(a,a.findViewById(R.id.root),source,Clock.fixed(NOW,ZoneOffset.UTC),()->online);a.rain=r;
        r.attachWeather(widget(a.findViewById(R.id.root)));r.resume();waitFor(r);return r;
    }
    void waitFor(RainController r)throws Exception{for(int i=0;i<400&&r.isBusy();i++){Thread.sleep(5);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertFalse("Weather worker timed out",r.isBusy());}
    @Test public void disabledKinarutFetchesPaparOnceWithoutEnablingRainAndPauses()throws Exception{
        RainOptions.prefs(RuntimeEnvironment.getApplication()).edit().putBoolean("enabled",false).commit();
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();RainController r=attach(c.get(),source,true);WeatherWidget w=widget(c.get().findViewById(R.id.root));
            assertEquals("Ds548",source.lastId);assertEquals(1,source.calls.get());assertEquals("Kinarut · 24–33°C",heading(w).getText().toString());
            assertFalse(r.view.effectVisible());assertFalse(new RainOptions(RainOptions.prefs(c.get())).enabled);assertEquals(RainOptions.HOME_ID,new RainOptions(RainOptions.prefs(c.get())).locationId);
            helper.render(c.get(),1920,1080);for(int i=0;i<15;i++)helper.render(c.get(),1920,1080);assertEquals(1,source.calls.get());
            assertTrue(r.hasWeatherWake());r.screen(false);assertFalse(r.hasWeatherWake());r.screen(true);assertTrue(r.hasWeatherWake());
            c.pause();assertFalse(r.hasWeatherWake());c.resume();waitFor(r);assertEquals(1,source.calls.get());
        }
    }
    @Test public void offlineMissingAndFreshCacheAndNetworkFailure()throws Exception{
        for(int scenario=0;scenario<5;scenario++)try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();if(scenario>0&&scenario<4)source.cache=KinarutWeatherTest.fixture(scenario==3?25*3600000L:7*3600000L);
            source.failure=new java.io.IOException("Simulated network failure");RainController r=attach(c.get(),source,scenario==2||scenario==4);
            WeatherWidget w=widget(c.get().findViewById(R.id.root));
            if(scenario==1||scenario==2){assertEquals("Kinarut · 24–33°C",heading(w).getText().toString());assertTrue(condition(w).getText().toString().endsWith(" · cache"));}
            else{assertEquals("Kinarut",heading(w).getText().toString());assertEquals("Ramalan tidak tersedia",condition(w).getText().toString());}
            assertEquals(scenario==2||scenario==4?1:0,source.calls.get());assertTrue(r.hasWeatherWake());assertTrue(r.nextFetchAt()>NOW.toEpochMilli());
        }
    }
    @Test public void freshDiskCacheIsLabelledEvenWithoutAnErrorOrNetworkRequest()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();
            source.cache=WeatherForecast.parse(KinarutWeatherTest.raw(),"Ds548",NOW.minusSeconds(1800).toEpochMilli());
            RainController r=attach(c.get(),source,true);WeatherWidget w=widget(c.get().findViewById(R.id.root));
            assertEquals(0,source.calls.get());
            assertTrue(condition(w).getText().toString().endsWith(" · cache"));
            assertTrue(r.status().contains("Last problem: None"));
            assertTrue(r.status().contains("Data origin: Cached / retained forecast"));
            assertTrue(w.getContentDescription().toString().contains(RainOptions.HOME_SCOPE));
            assertTrue(w.getContentDescription().toString().contains("Retrieved:"));
            transparent(w);
        }
    }
    @Test public void successfulRefreshReplacesCacheLabelWithoutChangingWeatherBounds()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();
            source.cache=WeatherForecast.parse(KinarutWeatherTest.raw(),"Ds548",NOW.minusSeconds(1800).toEpochMilli());
            RainController r=attach(c.get(),source,true);View root=helper.render(c.get(),1920,1080);
            WeatherWidget w=widget(root);Rect before=bounds(w);
            assertTrue(condition(w).getText().toString().endsWith(" · cache"));
            r.refresh();waitFor(r);helper.render(c.get(),1920,1080);
            assertEquals(1,source.calls.get());
            assertFalse(condition(w).getText().toString().endsWith(" · cache"));
            assertTrue(r.status().contains("Data origin: Network retrieval this session"));
            assertEquals(before,bounds(w));
        }
    }
    @Test public void failedRefreshLabelsPreviouslyFetchedMemoryDataAsRetainedCache()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();RainController r=attach(c.get(),source,true);
            WeatherWidget w=widget(c.get().findViewById(R.id.root));
            assertFalse(condition(w).getText().toString().endsWith(" · cache"));
            // Simulate the request gate elapsing without removing the production cooldown.
            RainOptions.prefs(c.get()).edit().remove("request_not_before").commit();
            source.failure=new java.io.IOException("Simulated refresh failure");r.refresh();waitFor(r);
            assertEquals(2,source.calls.get());
            assertTrue(condition(w).getText().toString().endsWith(" · cache"));
            assertTrue(r.status().contains("Data origin: Cached / retained forecast"));
            assertTrue(r.status().contains("Simulated refresh failure"));
        }
    }
    @Test public void noRepeatedTextLayoutAndTransparentOverBrightAndDarkWallpaper()throws Exception{
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
            View root=helper.render(c.get(),1920,1080);WeatherWidget w=widget(root);w.update(fixture(0),"St502","Sabah",NOW,false);helper.render(c.get(),1920,1080);
            w.update(fixture(0),"St502","Sabah",NOW,false);assertFalse("Unchanged data should not request text layout",w.isLayoutRequested());
            ImageView wallpaper=(ImageView)((FrameLayout)root).getChildAt(0);
            for(int color:new int[]{Color.WHITE,Color.BLACK}){wallpaper.setImageDrawable(new android.graphics.drawable.ColorDrawable(color));helper.capture(root,color==Color.WHITE?"weather-bright":"weather-dark");}
        }
    }
}
