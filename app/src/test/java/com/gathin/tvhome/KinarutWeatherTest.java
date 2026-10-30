package com.gathin.tvhome;

import android.os.Looper;
import android.widget.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class KinarutWeatherTest {
    static final Instant NOW=Instant.parse("2026-10-25T01:57:00Z");
    static String raw()throws Exception{return resource("/met-papar-Ds548-2026-10-25.json");}
    static String resource(String name)throws Exception{try(var in=KinarutWeatherTest.class.getResourceAsStream(name)){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
    static WeatherForecast fixture(long age)throws Exception{return WeatherForecast.parse(raw(),"Ds548",NOW.toEpochMilli()-age);}
    void waitFor(RainController controller)throws Exception{for(int i=0;i<500&&controller.isBusy();i++){Thread.sleep(5);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertFalse(controller.isBusy());}
    void error(String json,String expected)throws Exception{
        try{WeatherForecast.parse(json,"Ds548",NOW.toEpochMilli());fail("Accepted "+json);}catch(WeatherForecast.ParseFailure bad){assertTrue(bad.getMessage(),bad.getMessage().contains(expected));}
    }
    @Test public void documentedExactPaparUrlAndActualDefaultAndProjectedResponsesParse()throws Exception{
        assertEquals("https://api.data.gov.my/weather/forecast/?filter=Ds548%40location__location_id&sort=date&limit=14",MetWeatherRepository.endpoint("Ds548"));
        for(String json:new String[]{raw(),resource("/met-papar-Ds548-projected-2026-10-25.json")}){
            WeatherForecast f=WeatherForecast.parse(json,"Ds548",NOW.toEpochMilli());assertEquals(7,f.days.size());assertEquals("Papar",f.sourceName);assertEquals("Ds548",f.locationId);assertEquals("24–33°C",f.temperatureRange(NOW));assertEquals("Ribut petir di beberapa tempat",f.description(NOW));
        }
    }
    @Test public void realResponseReproducesEarlyMorningMissingPeriodButExplicitDailyFallbackWorks()throws Exception{
        Instant early=Instant.parse("2026-10-24T18:00:00Z"); // 02:00 MYT, requested date is Oct 24.
        WeatherForecast f=WeatherForecast.parse(raw(),"Ds548",early.toEpochMilli());
        assertEquals("",f.description(early));assertEquals(WeatherForecast.State.UNKNOWN,f.state(early));
        assertEquals("Ribut petir di beberapa tempat",f.displayDescription(early));assertEquals("Ramalan harian",f.displayLabel(early));
        assertTrue(f.availability(early).contains("2026-10-24 / night"));
        assertEquals(WeatherForecast.State.THUNDERSTORM_EXPECTED,f.displayState(early));
        // Projected response has no summary: never invent a period fallback.
        WeatherForecast projected=WeatherForecast.parse(resource("/met-papar-Ds548-projected-2026-10-25.json"),"Ds548",early.toEpochMilli());assertEquals("",projected.displayDescription(early));
    }
    @Test public void missingOptionalFieldsDoNotDiscardOtherPeriodsOrDays()throws Exception{
        JSONArray rows=new JSONArray(raw());JSONObject first=rows.getJSONObject(0);first.remove("morning_forecast");first.put("night_forecast",JSONObject.NULL);first.remove("min_temp");
        WeatherForecast f=WeatherForecast.parse(rows.toString(),"Ds548",NOW.toEpochMilli());assertEquals(7,f.days.size());assertEquals("",f.temperatureRange(NOW));assertEquals("",f.description(NOW));assertEquals("Ramalan harian",f.displayLabel(NOW));
        Instant afternoon=Instant.parse("2026-10-25T05:00:00Z");assertEquals("Ribut petir di kebanyakan tempat",f.description(afternoon));assertEquals("Ramalan",f.displayLabel(afternoon));
        rows.getJSONObject(1).put("date","bad-date");assertEquals(6,WeatherForecast.parse(rows.toString(),"Ds548",NOW.toEpochMilli()).days.size());
        first.remove("summary_forecast");f=WeatherForecast.parse(rows.toString(),"Ds548",NOW.toEpochMilli());assertEquals("",f.displayDescription(NOW));assertTrue(f.availability(NOW).contains("no daily summary"));
    }
    @Test public void malformedStructureEmptyWrongLocationAndNoDatesAreDistinguishable()throws Exception{
        error("not-json","Invalid JSON");error("[{","Invalid JSON");error("{}","Unexpected response structure");error("[]","No records returned");error("[{}]","missing location");
        JSONArray rows=new JSONArray(raw());for(int i=0;i<rows.length();i++)rows.getJSONObject(i).getJSONObject("location").put("location_id","St502");error(rows.toString(),"No records matching selected location Ds548");
        rows=new JSONArray(raw());for(int i=0;i<rows.length();i++)rows.getJSONObject(i).put("date","bad");error(rows.toString(),"no valid forecast dates");
    }
    @Test public void cacheAgeAndMissingPeriodDiagnosticsAreDistinct()throws Exception{
        assertTrue(fixture(WeatherForecast.FRESH_MILLIS).availability(NOW).contains("Expired cached data"));assertTrue(fixture(-1).availability(NOW).contains("future"));
        WeatherForecast f=fixture(0);assertTrue(f.availability(NOW).startsWith("Available"));
        Instant missing=NOW.plus(Duration.ofDays(20));f=WeatherForecast.parse(raw(),"Ds548",missing.toEpochMilli());assertTrue(f.availability(missing).contains("Missing forecast"));assertEquals("",f.displayDescription(missing));
    }
    @Test public void networkTimeoutHttpAndJsonDiagnosticsRemainDistinct(){
        assertTrue(RainController.diagnose(new java.net.SocketTimeoutException("read timed out")).startsWith("Network timeout"));
        assertTrue(RainController.diagnose(new java.net.UnknownHostException("host")).startsWith("Network/DNS"));
        assertTrue(RainController.diagnose(new MetWeatherRepository.FetchFailure(503,900000)).startsWith("HTTP/API error"));
        assertEquals("Invalid JSON",RainController.diagnose(new WeatherForecast.ParseFailure("Invalid JSON")));
    }
    @Test public void migrationSetsOnlyWeatherHomeOnceAndPreservesPrayerLocation(){
        var context=RuntimeEnvironment.getApplication();var prefs=RainOptions.prefs(context);prefs.edit().putString("location_id","St502").putString("location_name","Sabah").commit();
        context.getSharedPreferences("solat",0).edit().putString("zone","SWK08").commit();
        RainOptions.initializeHome(prefs);assertEquals("Ds548",new RainOptions(prefs).locationId);assertEquals("Kinarut",new RainOptions(prefs).locationName);assertEquals("SWK08",context.getSharedPreferences("solat",0).getString("zone",""));
        prefs.edit().putString("location_id","Ds554").putString("location_name","Kota Kinabalu").commit();RainOptions.initializeHome(prefs);assertEquals("Ds554",new RainOptions(prefs).locationId);
        RainOptions.useHome(prefs);assertEquals("Ds548",new RainOptions(prefs).locationId);
    }
    static class Source implements RainController.Source {
        WeatherForecast cache;Exception failure;AtomicInteger calls=new AtomicInteger();
        public WeatherForecast cached(String id){return cache!=null&&id.equals(cache.locationId)?cache:null;}
        public WeatherForecast fetch(String id,long stamp)throws Exception{calls.incrementAndGet();if(failure!=null)throw failure;return WeatherForecast.parse(raw(),id,stamp);}
    }
    RainController attach(DashboardActivity activity,Source source,Instant now,boolean online)throws Exception{
        activity.rain.destroy();RainOptions.prefs(activity).edit().remove("retry_at").remove("request_not_before").commit();
        RainController controller=new RainController(activity,activity.findViewById(R.id.root),source,Clock.fixed(now,ZoneOffset.UTC),()->online);activity.rain=controller;
        controller.attachWeather(activity.findViewById(R.id.root).findViewWithTag("weather-widget"));controller.resume();waitFor(controller);return controller;
    }
    @Test public void controllerDisplaysRealPaparDataAndConciseOfflineState()throws Exception{
        for(boolean online:new boolean[]{true,false})try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();RainController c=attach(activity.get(),source,NOW,online);
            WeatherWidget widget=activity.get().findViewById(R.id.root).findViewWithTag("weather-widget");WeatherWidgetTest helper=new WeatherWidgetTest();
            if(online){assertEquals("Kinarut, Sabah",helper.heading(widget).getText().toString());assertTrue(helper.condition(widget).getText().toString().startsWith("24–33°"));assertTrue(helper.condition(widget).getText().toString().contains("Ribut petir"));assertEquals(1,source.calls.get());}
            else{assertEquals("Ramalan tidak tersedia",helper.condition(widget).getText().toString());assertTrue(c.status().contains("Offline; no request sent"));assertEquals(0,source.calls.get());}
            assertTrue(c.status().contains(RainOptions.HOME_SCOPE));
        }
    }
    @Test public void futureDatedCacheDoesNotDeferRefreshUntilItsFutureTimestamp()throws Exception{
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();source.cache=fixture(-3600000);RainController c=attach(activity.get(),source,NOW,true);assertEquals(1,source.calls.get());assertTrue(c.status().contains("Available:"));
        }
    }
    @Test public void dateGapUsesLabelledSummaryInWidgetButDoesNotStartRainAnimation()throws Exception{
        Instant early=Instant.parse("2026-10-24T18:00:00Z");try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            RainOptions.prefs(activity.get()).edit().putBoolean("enabled",true).putBoolean("forecast",true).commit();
            Source source=new Source();RainController c=attach(activity.get(),source,early,true);WeatherWidget widget=activity.get().findViewById(R.id.root).findViewWithTag("weather-widget");
            assertTrue(new WeatherWidgetTest().periods(widget).getText().toString().startsWith("MET harian"));assertEquals(WeatherForecast.State.UNKNOWN,c.currentState());assertFalse(c.view.effectVisible());
            new DashboardTest().capture(new DashboardTest().render(activity.get(),1920,1080),"kinarut-daily-fallback-1080");
        }
    }
    @Test public void locationChangeDuringFetchCannotPublishOldLocation()throws Exception{
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);Source source=new Source(){public WeatherForecast fetch(String id,long stamp)throws Exception{started.countDown();release.await(2,TimeUnit.SECONDS);return super.fetch(id,stamp);}};
            activity.get().rain.destroy();RainOptions.prefs(activity.get()).edit().remove("request_not_before").remove("retry_at").commit();
            RainController c=new RainController(activity.get(),activity.get().findViewById(R.id.root),source,Clock.fixed(NOW,ZoneOffset.UTC),()->true);activity.get().rain=c;
            c.attachWeather(activity.get().findViewById(R.id.root).findViewWithTag("weather-widget"));c.resume();assertTrue(started.await(1,TimeUnit.SECONDS));
            RainOptions.prefs(activity.get()).edit().putString("location_id","Ds554").putString("location_name","Kota Kinabalu").commit();Shadows.shadowOf(Looper.getMainLooper()).idle();release.countDown();waitFor(c);
            WeatherWidget widget=activity.get().findViewById(R.id.root).findViewWithTag("weather-widget");assertEquals("Kota Kinabalu",new WeatherWidgetTest().heading(widget).getText().toString());assertTrue(c.status().contains("Ds554"));
        }
    }
    @Test public void locationMenuExplainsRegionalProxyInsteadOfOpeningCatalogue()throws Exception{
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            activity.get().rain.showSettings();var dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getListView().performItemClick(null,1,1);dialog.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();
            var home=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertNull(home.getListView());assertTrue(org.robolectric.Shadows.shadowOf(home).getMessage().toString().contains("Papar district"));
        }
    }
    @Test public void controllerReportsEachFailureWithoutCrashingOrInventingWeather()throws Exception{
        Exception[] failures={new java.net.SocketTimeoutException("read timed out"),new MetWeatherRepository.FetchFailure(503,900000),new WeatherForecast.ParseFailure("Invalid JSON in MET response"),new WeatherForecast.ParseFailure("No records matching selected location Ds548"),new WeatherForecast.ParseFailure("Unexpected response structure: expected JSON array")};
        for(Exception failure:failures)try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();source.failure=failure;RainController c=attach(activity.get(),source,NOW,true);
            assertTrue(c.status(),c.status().contains(RainController.diagnose(failure)));assertEquals(1,source.calls.get());assertTrue(c.nextFetchAt()>NOW.toEpochMilli());
            WeatherWidget widget=activity.get().findViewById(R.id.root).findViewWithTag("weather-widget");assertEquals("Ramalan tidak tersedia",new WeatherWidgetTest().condition(widget).getText().toString());
        }
    }
    @Test public void missingPeriodInLegacyProjectedCacheRetriesSoonerThanSixHours()throws Exception{
        Instant early=Instant.parse("2026-10-24T18:00:00Z");try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            Source source=new Source();source.cache=WeatherForecast.parse(resource("/met-papar-Ds548-projected-2026-10-25.json"),"Ds548",early.minusSeconds(1800).toEpochMilli());
            RainController c=attach(activity.get(),source,early,true);assertEquals(1,source.calls.get());assertTrue(c.status().contains("showing today's DAILY summary"));
        }
    }

}
