package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HomeRefreshTest {
    static final long NOW=Instant.parse("2026-10-28T06:30:00Z").toEpochMilli();
    private final DashboardTest helper=new DashboardTest();
    String current()throws Exception{return new JSONObject().put("current_units",new JSONObject().put("time","unixtime").put("temperature_2m","°C")).put("current",new JSONObject().put("time",NOW/1000).put("temperature_2m",29.4).put("weather_code",3)).toString();}
    void pump(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));}
    @Test public void acceptsCurrentOnlyWeatherWithoutForecastArrays()throws Exception{HourlyWeather w=HourlyWeather.parseCurrent(current(),NOW);assertEquals(29.4,w.current(Instant.ofEpochMilli(NOW)).temperature,0);assertTrue(w.hours.isEmpty());assertFalse(HourlyWeatherRepository.endpoint(0,0).contains("hourly="));}
    @Test public void rejectsInvalidCurrentUnitsTimestampAndTemperature()throws Exception{for(String key:new String[]{"temperature_2m","time","units"}){JSONObject json=new JSONObject(current());if(key.equals("units"))json.getJSONObject("current_units").put("temperature_2m","°F");else json.getJSONObject("current").put(key,key.equals("time")?NOW/1000-9000:JSONObject.NULL);try{HourlyWeather.parseCurrent(json.toString(),NOW);fail(key);}catch(JSONException expected){}}}
    @Test public void quoteParserKeepsOnlyShortPlainTextWithAuthor()throws Exception{String longText="a".repeat(111);String raw=new JSONArray().put(new JSONObject().put("q","A kind word matters.").put("a","Example")).put(new JSONObject().put("q",longText).put("a","Example")).put(new JSONObject().put("q","<b>unsafe</b>").put("a","Example")).put(new JSONObject().put("q","Missing author.")).toString();assertEquals(1,DailyQuotes.parse(raw).size());assertEquals("Example",DailyQuotes.parse(raw).get(0).author);}
    @Test public void quoteSelectionIsStableWithinLocalDayAndRotates(){var values=Arrays.asList(new DailyQuotes.Quote("One short quote.","A"),new DailyQuotes.Quote("Another short quote.","B"));LocalDate day=LocalDate.of(2026,10,28);assertSame(DailyQuotes.choose(values,day),DailyQuotes.choose(values,day));assertNotSame(DailyQuotes.choose(values,day),DailyQuotes.choose(values,day.plusDays(1)));assertFalse(DailyQuotes.choose(Collections.emptyList(),day).text.isEmpty());}
    @Test public void quoteCacheFetchesOncePerDayAndStopsWhenPaused()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        DashboardActivity a=c.get();a.getSharedPreferences(DailyQuotes.PREFS,0).edit().putBoolean("enabled",true).commit();AtomicInteger hits=new AtomicInteger();TextView text=new TextView(a),credit=new TextView(a);
        DailyQuotes q=new DailyQuotes(a,text,credit,()->{hits.incrementAndGet();return "[{\"q\":\"Make today a little kinder.\",\"a\":\"Example\"}]";},Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));
        try{q.resume();for(int i=0;i<300&&!credit.getText().toString().contains("ZenQuotes");i++){Thread.sleep(5);pump();}assertTrue(credit.getText().toString().contains("ZenQuotes"));assertEquals(1,hits.get());q.resume();pump();assertEquals(1,hits.get());q.pause();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2));assertEquals(1,hits.get());}finally{q.destroy();}
    }}
    @Test public void quoteFailureBacksOffAndKeepsOfflineReflection()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
        DashboardActivity a=c.get();a.getSharedPreferences(DailyQuotes.PREFS,0).edit().putBoolean("enabled",true).commit();AtomicInteger hits=new AtomicInteger();TextView text=new TextView(a),credit=new TextView(a);DailyQuotes q=new DailyQuotes(a,text,credit,()->{hits.incrementAndGet();throw new IOException("offline");},Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));
        try{q.resume();for(int i=0;i<100&&hits.get()==0;i++){Thread.sleep(5);pump();}q.resume();assertEquals(1,hits.get());assertTrue(credit.getText().toString().contains("FayyadhOS"));assertFalse(text.getText().toString().isEmpty());}finally{q.destroy();}
    }}
    @Test public void iconPackMappingNormalizesRelativeActivitiesAndRejectsUnsafeDrawableNames()throws Exception{
        var parser=XmlPullParserFactory.newInstance().newPullParser();parser.setInput(new StringReader("<resources><item component=\"ComponentInfo{com.example/.Main}\" drawable=\"full_banner\"/><item component=\"ComponentInfo{com.bad/.Main}\" drawable=\"../bad\"/></resources>"));Map<String,String> map=AppArtwork.parse(parser);assertEquals("full_banner",map.get("com.example/com.example.Main"));assertEquals(1,map.size());
    }
    @Test public void missingPackFallsBackToInstalledAppArtwork(){helper.demoApps();Context a=RuntimeEnvironment.getApplication();a.getSharedPreferences(AppArtwork.PREFS,0).edit().putString("pack","missing.icon.pack").commit();assertNotNull(new AppArtwork(a).load(new DashboardApps(a).visible().get(0)));}
    private void preview(int w,int h,String suffix)throws Exception{
        helper.demoApps();helper.seedApiCache();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
            DashboardActivity a=c.get();View root=helper.render(a,w,h);TextView greeting=root.findViewWithTag("greeting"),brand=root.findViewWithTag("wordmark");assertTrue(brand.getTypeface().isItalic());assertTrue(brand.getTypeface().isBold());assertTrue(greeting.getText().toString().matches("Good (Morning|Afternoon|Evening|Night)"));assertTrue(((View)greeting.getParent()).getTop()>=a.d(148));
            a.getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putBoolean("enabled",true).putString("latitude","0").putString("longitude","0").putString("label","Demo location").commit();
            HourlyWeather fixture=HourlyWeather.parseCurrent(current(),NOW);HourlyWeatherController weather=new HourlyWeatherController(a,new HourlyWeatherController.Source(){public HourlyWeather cached(double x,double y){return fixture;}public HourlyWeather fetch(double x,double y,long now){fail("No live weather in screenshots");return null;}},Clock.fixed(Instant.ofEpochMilli(NOW),ZoneOffset.UTC));
            try{View old=root.findViewWithTag("hourly-weather");ViewGroup parent=(ViewGroup)old.getParent();ViewGroup.LayoutParams params=old.getLayoutParams();parent.removeView(old);parent.addView(weather.view(),params);weather.resume();for(int i=0;i<200&&!weather.view().getContentDescription().toString().contains("cache");i++){Thread.sleep(5);pump();}
                assertFalse(weather.view().getContentDescription().toString().contains("+3"));assertFalse(allText(root).contains("Cuaca jam:"));assertFalse(allText(root).contains("entertainment"));
                // Synthetic native banner fixture, not a bundled third-party brand or a device capture.
                RecyclerView grid=root.findViewById(R.id.grid);for(int i=0;i<grid.getChildCount();i++){View tile=grid.getChildAt(i);ImageView image=tile.findViewWithTag("app-artwork");if(image!=null){Bitmap b=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xff163b52);Paint paint=new Paint(3);paint.setColor(0xffb8f4ec);paint.setTextSize(36);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("APP BANNER",160,102,paint);image.setImageBitmap(b);}}
                TextView note=GlassUi.text(a,"SIMULATION · synthetic app banners / weather fixtures · NOT a TV capture",11,Color.WHITE);note.setBackgroundColor(0xb0000000);FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER);((FrameLayout)root).addView(note,np);helper.render(a,w,h);helper.capture(root,"home-refresh-"+suffix);
                assertTrue(grid.getTop()>h*.72);assertTrue(grid.getBottom()<h*.97);a.showJarvis();Dialog d=ShadowDialog.getLatestDialog();View decor=d.getWindow().getDecorView();WindowManager.LayoutParams wp=d.getWindow().getAttributes();decor.measure(View.MeasureSpec.makeMeasureSpec(wp.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(wp.height,View.MeasureSpec.EXACTLY));decor.layout(0,0,wp.width,wp.height);
                Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(out);root.draw(cv);cv.save();cv.translate(w-wp.x-wp.width,wp.y);decor.draw(cv);cv.restore();File file=new File(System.getProperty("user.dir"),"build/control-refresh-"+suffix+".png");try(var stream=new FileOutputStream(file)){out.compress(Bitmap.CompressFormat.PNG,100,stream);}out.recycle();d.dismiss();
            }finally{weather.destroy();}
        }
    }
    String allText(View v){String result=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)result+=" "+allText(((ViewGroup)v).getChildAt(i));return result;}
    @Test public void preview1080()throws Exception{preview(1920,1080,"1080");}
    @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void preview720()throws Exception{preview(1280,720,"720");}
    @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void preview4k()throws Exception{preview(3840,2160,"4k");}
}
