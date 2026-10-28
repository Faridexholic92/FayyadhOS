package com.gathin.tvhome;

import android.app.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import java.io.*;import java.lang.reflect.*;import java.nio.file.*;import java.time.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PrayerWeatherUpgradeTest {
 static class Time extends Clock {Instant value=Instant.parse("2026-10-25T04:30:00Z");public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return value;}}
 final DashboardTest helper=new DashboardTest();
 String official()throws Exception{try(var in=getClass().getResourceAsStream("/jakim-SBH07-2026-10-OFFICIAL-CAPTURE.json")){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
 Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 void render(PrayerPanel p)throws Exception{Method m=PrayerPanel.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(p);}
 void idle(long ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @Test public void officialCaptureParsesAllThirtyOneDaysAndExactZone()throws Exception{Map<LocalDate,PrayerTimes.Day> map=PrayerTimes.parse(official(),"SBH07",YearMonth.of(2026,10));assertEquals(31,map.size());assertEquals("04:52",PrayerTimes.hhmm(map.get(LocalDate.of(2026,10,25)).times[0]));}
 @Test public void cacheRevalidatesAtMalaysiaMidnightNotRolling24Hours()throws Exception{
  Time clock=new Time();clock.value=Instant.parse("2026-10-25T15:59:59Z");AtomicInteger calls=new AtomicInteger();
  PrayerRepository r=new PrayerRepository(RuntimeEnvironment.getApplication(),clock,(z,m)->{calls.incrementAndGet();return official();});
  assertEquals(31,r.fetch("SBH07",YearMonth.of(2026,10)).size());assertEquals(1,calls.get());r.fetch("SBH07",YearMonth.of(2026,10));assertEquals(1,calls.get());
  clock.value=Instant.parse("2026-10-25T16:00:00Z");r.fetch("SBH07",YearMonth.of(2026,10));assertEquals(2,calls.get());
 }
 @Test public void invalidApiCannotOverwriteTodaysOfflineCache()throws Exception{Time clock=new Time();AtomicInteger n=new AtomicInteger();PrayerRepository r=new PrayerRepository(RuntimeEnvironment.getApplication(),clock,(z,m)->{if(n.incrementAndGet()==1)return official();return "<html>Unavailable</html>";});
  r.fetch("SBH07",YearMonth.of(2026,10));try{r.fetch("SBH07",YearMonth.of(2026,10),true);fail();}catch(Exception expected){}
  assertEquals(31,r.cached("SBH07",YearMonth.of(2026,10)).size());
 }
 @Test public void offlineFailureDoesNotDeleteSavedCache()throws Exception{Time clock=new Time();PrayerRepository r=new PrayerRepository(RuntimeEnvironment.getApplication(),clock,(z,m)->official());r.fetch("SBH07",YearMonth.of(2026,10));
  PrayerRepository offline=new PrayerRepository(RuntimeEnvironment.getApplication(),clock,(z,m)->{throw new java.io.IOException("test offline");});try{offline.fetch("SBH07",YearMonth.of(2026,10),true);fail();}catch(java.io.IOException expected){}
  assertEquals("19:10",PrayerTimes.hhmm(offline.cached("SBH07",YearMonth.of(2026,10)).get(LocalDate.of(2026,10,25)).times[5]));
 }
 @Test public void badgeTransitionsAtExactPrayerAndSunriseBoundaries()throws Exception{PrayerTimes.Day d=PrayerTimes.parse(official(),"SBH07",YearMonth.of(2026,10)).get(LocalDate.of(2026,10,25));
  String[] times={"04:51:59","04:52:00","06:00:59","06:01:00","12:01:59","12:02:00","15:21:00","17:59:00","19:10:00","23:59:59"};int[] expected={-1,0,0,-1,-1,2,3,4,5,5};
  for(int i=0;i<times.length;i++)assertEquals(times[i],expected[i],PrayerTimes.current(d,LocalDateTime.parse("2026-10-25T"+times[i]).atZone(PrayerTimes.MALAYSIA).toInstant()));
  assertEquals(-1,PrayerTimes.current(d,Instant.parse("2026-10-25T16:00:00Z")));assertEquals(-1,PrayerTimes.current(null,Instant.now()));
 }
 @Test public void timerTargetsExactBoundaryAndMidnight()throws Exception{PrayerTimes.Day d=PrayerTimes.parse(official(),"SBH07",YearMonth.of(2026,10)).get(LocalDate.of(2026,10,25));
  assertEquals(1000,PrayerTimes.nextTickMillis(d,Instant.parse("2026-10-25T04:01:59Z")));assertEquals(1000,PrayerTimes.nextTickMillis(d,Instant.parse("2026-10-25T15:59:59Z")));assertEquals(30000,PrayerTimes.nextTickMillis(null,Instant.parse("2026-10-25T04:00:00Z")));
 }
 @Test public void incompleteMonthCannotReplaceGoodCache()throws Exception{Time clock=new Time();AtomicInteger n=new AtomicInteger();PrayerRepository r=new PrayerRepository(RuntimeEnvironment.getApplication(),clock,(z,m)->{if(n.incrementAndGet()==1)return official();org.json.JSONObject bad=new org.json.JSONObject(official());bad.getJSONArray("prayerTime").remove(0);return bad.toString();});r.fetch("SBH07",YearMonth.of(2026,10));try{r.fetch("SBH07",YearMonth.of(2026,10),true);fail();}catch(java.io.IOException expected){}assertEquals(31,r.cached("SBH07",YearMonth.of(2026,10)).size());}
 @Test public void futureClockCacheIsNotFresh(){assertFalse(PrayerRepository.fresh(1001,Instant.ofEpochMilli(1000)));assertFalse(PrayerRepository.fresh(0,Instant.now()));}
 PrayerPanel configured(Activity activity,Time clock)throws Exception{
  activity.getSharedPreferences("solat",0).edit().putString("zone","SBH07").commit();NetworkFocus.enable(activity);
  PrayerRepository r=new PrayerRepository(activity,clock,(z,m)->official());r.fetch("SBH07",YearMonth.of(2026,10),true);
  PrayerPanel panel=new PrayerPanel(activity,clock);panel.dashboardScale(1);return panel;
 }
 @Test public void fiveTimesDoNotRotateWhenTimePassesOrRemoteArrowsArePressed()throws Exception{try(var c=Robolectric.buildActivity(Activity.class).setup().visible()){
  PrayerPanel p=configured(c.get(),new Time());c.get().setContentView(p);p.start();idle(10);ViewGroup row=p.findViewWithTag("fivePrayerRow");assertEquals(5,row.getChildCount());
  String before=p.getContentDescription().toString();idle(16000);assertEquals(before,p.getContentDescription().toString());assertFalse(p.cycle(1));assertEquals(before,p.getContentDescription().toString());p.destroy();
 }}
 @Test public void currentNameIsIndependentOfFocusAndMissingDayClearsEveryTime()throws Exception{try(var c=Robolectric.buildActivity(Activity.class).setup()){
  Time clock=new Time();PrayerPanel p=configured(c.get(),clock);c.get().setContentView(p);p.start();idle(10);assertEquals(2,field(p,"glowingIndex"));p.requestFocus();render(p);assertEquals(2,field(p,"glowingIndex"));
  clock.value=Instant.parse("2026-12-01T04:30:00Z");render(p);for(TextView t:(TextView[])field(p,"values"))assertEquals("--:--",t.getText().toString());assertEquals(-1,field(p,"glowingIndex"));p.destroy();
 }}
 @Test public void newDefaultsAutoRainButExistingOffAndLightningOptInAreRespected(){var prefs=RainOptions.prefs(RuntimeEnvironment.getApplication());RainOptions o=new RainOptions(prefs);assertTrue(o.enabled);assertTrue(o.forecastDriven);assertFalse(o.lightning);prefs.edit().putBoolean("enabled",false).putBoolean("forecast",false).commit();o=new RainOptions(prefs);assertFalse(o.enabled);assertFalse(o.forecastDriven);}
 @Test public void lightningRequiresOptInThunderAndMotionAndRespectsReducedMotion(){var ctx=RuntimeEnvironment.getApplication();var prefs=RainOptions.prefs(ctx);RainGlassView view=new RainGlassView(ctx);view.configure(new RainOptions(prefs));view.setThunder(true);assertFalse(view.thunderEnabled());prefs.edit().putBoolean("lightning",true).commit();view.configure(new RainOptions(prefs));assertTrue(view.thunderEnabled());view.setThunder(false);assertFalse(view.thunderEnabled());view.setThunder(true);android.provider.Settings.Global.putFloat(ctx.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,0);view.configure(new RainOptions(prefs));assertFalse(view.thunderEnabled());assertEquals(0,view.activeMovingCount());assertTrue(view.streakCount()<=48);view.destroy();}
 @Test public void clearHomeAndStormNativePreviewsAreExplicitFixtures()throws Exception{helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  DashboardActivity a=c.get();View root=helper.render(a,1920,1080);PrayerPanel old=a.findViewById(R.id.prayerPanel);old.destroy();ViewGroup body=(ViewGroup)old.getParent();ViewGroup.LayoutParams params=old.getLayoutParams();body.removeView(old);Time clock=new Time();PrayerPanel p=configured(a,clock);body.addView(p,params);p.start();p.obscured(true);// All five visible; current Zohor follows the fixture clock.
  ((TextView)field(a,"clock")).setText("12:30 PM");((TextView)field(a,"date")).setText("Sunday, 25 Oct 2026");((TextView)field(a,"greeting")).setText("Good afternoon");
  TextView mark=new TextView(a);mark.setText("PRATONTON • 25 Okt 2026 • aplikasi & cuaca simulasi • bukan tangkapan TV sebenar");mark.setTextColor(Color.WHITE);mark.setTextSize(12);mark.setBackgroundColor(0xd0041720);FrameLayout.LayoutParams mp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);mp.bottomMargin=36;((FrameLayout)root).addView(mark,mp);
  helper.render(a,1920,1080);
  WeatherWidget weather=root.findViewWithTag("weather-widget");
  weather.update(WeatherForecastTest.data("Tiada Hujan",0),WeatherForecastTest.ID,"Cuaca simulasi",WeatherForecastTest.NOW,false);
  helper.capture(root,"prayer-clear-FIXTURE");
  var prefs=RainOptions.prefs(a);prefs.edit().putBoolean("enabled",true).putBoolean("forecast",false).putBoolean("lightning",true).putInt("density",2).commit();a.rain.preview(true);idle(900);weather.update(WeatherForecastTest.data("Hujan",0),WeatherForecastTest.ID,"Cuaca simulasi",WeatherForecastTest.NOW,false);helper.capture(root,"prayer-rain-FIXTURE");
  RainGlassView v=a.rain.view;v.setThunder(true);Field strength=RainGlassView.class.getDeclaredField("boltStrength");strength.setAccessible(true);strength.set(v,1f);weather.update(WeatherForecastTest.data("Ribut petir",0),WeatherForecastTest.ID,"Cuaca simulasi",WeatherForecastTest.NOW,false);helper.capture(root,"prayer-storm-FIXTURE");p.destroy();
 }}
}
