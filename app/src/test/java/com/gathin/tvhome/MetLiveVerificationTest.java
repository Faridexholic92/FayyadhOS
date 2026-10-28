package com.gathin.tvhome;

import android.os.Looper;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Explicit opt-in real HTTPS integration test, not a fixture-backed success claim. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MetLiveVerificationTest {
    @Test public void liveRepositoryParsesPaparCachesAndDisplaysKinarut()throws Exception{
        Assume.assumeTrue("Set MET_VERIFY_LIVE=1 to make an actual HTTPS request","1".equals(System.getenv("MET_VERIFY_LIVE")));
        var context=RuntimeEnvironment.getApplication();MetWeatherRepository source=new MetWeatherRepository(context);Instant now=Instant.now();
        ExecutorService worker=Executors.newSingleThreadExecutor();WeatherForecast forecast;
        try{forecast=worker.submit(()->source.fetch("Ds548",now.toEpochMilli())).get(45,TimeUnit.SECONDS);}finally{worker.shutdownNow();}
        assertEquals("Ds548",forecast.locationId);assertEquals("Papar",forecast.sourceName);assertTrue(forecast.days.size()>0);assertFalse(forecast.displayDescription(now).isEmpty());
        assertTrue(source.diagnostic().contains("HTTP 200"));assertNotNull(source.cached("Ds548"));assertNull(source.cached("St502"));assertNotNull(source.cached("Ds548"));
        new DashboardTest().demoApps();try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
            activity.get().rain.destroy();RainController controller=new RainController(activity.get(),activity.get().findViewById(R.id.root),source,Clock.fixed(now,ZoneOffset.UTC),()->true);activity.get().rain=controller;
            controller.attachWeather(activity.get().findViewById(R.id.root).findViewWithTag("weather-widget"));controller.resume();
            new KinarutWeatherTest().waitFor(controller);
            WeatherWidget widget=activity.get().findViewById(R.id.root).findViewWithTag("weather-widget");WeatherWidgetTest helper=new WeatherWidgetTest();
            assertTrue(helper.heading(widget).getText().toString().startsWith("Kinarut"));assertFalse(helper.condition(widget).getText().toString().equals("Ramalan tidak tersedia"));
            var root=new DashboardTest().render(activity.get(),1920,1080);new DashboardTest().capture(root,"kinarut-live-1080");
            JSONObject result=new JSONObject().put("requested_at",now.toString()).put("url",MetWeatherRepository.endpoint("Ds548"))
                    .put("transport",source.diagnostic()).put("record_count",forecast.days.size()).put("location_id",forecast.locationId).put("location_name",forecast.sourceName)
                    .put("display_heading",helper.heading(widget).getText()).put("display_condition",helper.condition(widget).getText()).put("diagnostics",controller.status());
            File out=new File(System.getProperty("user.dir"),"build/kinarut-live-result.json");out.getParentFile().mkdirs();Files.write(out.toPath(),result.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
}
