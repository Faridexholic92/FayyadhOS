package com.gathin.tvhome;
import android.animation.ValueAnimator;import android.app.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.GraphicsMode;
import java.lang.reflect.*;import java.time.*;import java.io.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FivePrayerGlowTest {
 final DashboardTest helper=new DashboardTest();final PrayerWeatherUpgradeTest fixture=new PrayerWeatherUpgradeTest();
 Object field(Object o,String n)throws Exception{return fixture.field(o,n);}
 TextView[] labels(PrayerPanel p)throws Exception{return (TextView[])field(p,"labels");}TextView[] values(PrayerPanel p)throws Exception{return (TextView[])field(p,"values");}
 void tick(PrayerPanel p,PrayerWeatherUpgradeTest.Time clock,String local)throws Exception{clock.value=LocalDateTime.parse("2026-10-25T"+local).atZone(PrayerTimes.MALAYSIA).toInstant();fixture.render(p);}
 @Test public void fiveOrderedColumnsTransparentAndNoSyurukOrMetadataOnHome()throws Exception{try(var a=Robolectric.buildActivity(Activity.class).setup().visible()){
  PrayerPanel p=fixture.configured(a.get(),new PrayerWeatherUpgradeTest.Time());a.get().setContentView(p);p.start();JarvisRepairTest.pump();ViewGroup row=p.findViewWithTag("fivePrayerRow");assertEquals(5,row.getChildCount());
  String[] names={"Subuh","Zohor","Asar","Maghrib","Isyak"};for(int i=0;i<5;i++)assertEquals(names[i],((TextView)((ViewGroup)row.getChildAt(i)).getChildAt(0)).getText().toString());
  assertNull(labels(p)[1].getParent());assertNull(((TextView)field(p,"heading")).getParent());assertNull(((TextView)field(p,"subtitle")).getParent());assertEquals(Color.TRANSPARENT,((GradientDrawable)p.getBackground()).getColor().getDefaultColor());
  assertEquals("04:52",values(p)[0].getText().toString());assertEquals("12:02",values(p)[2].getText().toString());assertEquals("19:10",values(p)[5].getText().toString());p.destroy();
 }}
 @Test public void onlyCurrentNameGlowsAndExactBoundariesMoveIt()throws Exception{try(var a=Robolectric.buildActivity(Activity.class).setup().visible()){
  var clock=new PrayerWeatherUpgradeTest.Time();PrayerPanel p=fixture.configured(a.get(),clock);a.get().setContentView(p);p.start();JarvisRepairTest.pump();
  String[] times={"04:51:59","04:52:00","06:01:00","12:02:00","15:20:59","15:21:00","17:59:00","19:10:00"};int[] selected={-1,0,-1,2,2,3,4,5};
  for(int j=0;j<times.length;j++){tick(p,clock,times[j]);assertEquals(selected[j],field(p,"glowingIndex"));for(int i:PrayerTimes.PRAYERS){assertEquals(i==selected[j]?GlassUi.CYAN:GlassUi.WHITE,labels(p)[i].getCurrentTextColor());assertEquals(GlassUi.WHITE,values(p)[i].getCurrentTextColor());}}
  p.destroy();
 }}
 @Test public void pulseStopsForOverlayBackgroundHiddenViewAndReducedMotion()throws Exception{try(var a=Robolectric.buildActivity(Activity.class).setup().visible()){
  PrayerPanel p=fixture.configured(a.get(),new PrayerWeatherUpgradeTest.Time());a.get().setContentView(p);p.start();JarvisRepairTest.pump();assertNotNull(field(p,"glowAnimator"));
  p.obscured(true);assertNull(field(p,"glowAnimator"));p.obscured(false);assertNotNull(field(p,"glowAnimator"));p.setVisibility(View.GONE);assertNull(field(p,"glowAnimator"));p.setVisibility(View.VISIBLE);assertNotNull(field(p,"glowAnimator"));
  android.provider.Settings.Global.putFloat(a.get().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,0);fixture.render(p);assertNull(field(p,"glowAnimator"));assertEquals(GlassUi.CYAN,labels(p)[2].getCurrentTextColor());
  android.provider.Settings.Global.putFloat(a.get().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1);fixture.render(p);assertNotNull(field(p,"glowAnimator"));p.stop();assertNull(field(p,"glowAnimator"));p.destroy();
 }}
 @Test public void missingCurrentDayOrZoneDoesNotInventTimesOrGlow()throws Exception{try(var a=Robolectric.buildActivity(Activity.class).setup().visible()){
  PrayerPanel empty=new PrayerPanel(a.get());empty.dashboardScale(1);a.get().setContentView(empty);empty.start();JarvisRepairTest.pump();for(int i:PrayerTimes.PRAYERS)assertEquals("--:--",values(empty)[i].getText().toString());assertEquals(-1,field(empty,"glowingIndex"));assertTrue(empty.getContentDescription().toString().contains("Pilih zon"));empty.destroy();
 }}
 void capture(View root,Dialog dialog,String name,int w,int h)throws Exception{
  Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);root.draw(c);
  if(dialog!=null){View decor=dialog.getWindow().getDecorView();var p=dialog.getWindow().getAttributes();decor.measure(View.MeasureSpec.makeMeasureSpec(p.width,1073741824),View.MeasureSpec.makeMeasureSpec(p.height,1073741824));decor.layout(0,0,p.width,p.height);c.save();c.translate(w-p.width-p.x,p.y);decor.draw(c);c.restore();}
  Paint paint=new Paint(3);paint.setColor(0xdd041522);c.drawRect(0,h-h*.039f,w*.74f,h,paint);paint.setTextSize(w/1920f*20);paint.setColor(Color.WHITE);c.drawText("PRATONTON UI ANDROID · jadual / tarikh / cuaca / aplikasi contoh · bukan tangkapan TV sebenar",w/64f,h-h*.013f,paint);
  File file=new File(System.getProperty("user.dir"),"build/five-prayer-previews/"+name+".png");file.getParentFile().mkdirs();try(var out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
 }
 void preview(int w,int h,String suffix,boolean animation)throws Exception{
  helper.demoApps();try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   DashboardActivity activity=a.get();View root=helper.render(activity,w,h);PrayerPanel old=activity.findViewById(R.id.prayerPanel);old.destroy();ViewGroup parent=(ViewGroup)old.getParent();ViewGroup.LayoutParams lp=old.getLayoutParams();parent.removeView(old);
   var clock=new PrayerWeatherUpgradeTest.Time();PrayerPanel p=fixture.configured(activity,clock);p.dashboardScale(w/1920f);parent.addView(p,lp);Field f=DashboardActivity.class.getDeclaredField("prayer");f.setAccessible(true);f.set(activity,p);p.start();helper.render(activity,w,h);
   ((TextView)field(activity,"clock")).setText("12:30 PM");((TextView)field(activity,"date")).setText("Sunday, 25 Oct 2026");((TextView)field(activity,"greeting")).setText("Good afternoon");
   WeatherWidget weather=root.findViewWithTag("weather-widget");weather.update(WeatherForecastTest.data("Tiada Hujan",0),WeatherForecastTest.ID,"Kinarut · simulasi",WeatherForecastTest.NOW,false);
   View row=p.findViewWithTag("fivePrayerRow");assertEquals(5,((ViewGroup)row).getChildCount());assertTrue(p.getBottom()<=root.findViewWithTag("greeting").getTop()+((View)root.findViewWithTag("greeting").getParent()).getTop());
   for(int index:PrayerTimes.PRAYERS){TextView t=values(p)[index];assertTrue("Unclipped time "+index,t.getWidth()>=t.getPaint().measureText(t.getText().toString()));}
   ValueAnimator pulse=(ValueAnimator)field(p,"glowAnimator");assertNotNull(pulse);pulse.setCurrentPlayTime(0);capture(root,null,"home-low-"+suffix,w,h);pulse.setCurrentPlayTime(1800);capture(root,null,"home-glow-"+suffix,w,h);
   if(animation){for(int i=0;i<36;i++){pulse.setCurrentPlayTime(i*100L);capture(root,null,String.format(java.util.Locale.ROOT,"glow-%03d",i),w,h);}}
   tick(p,clock,"15:21:00");((TextView)field(activity,"clock")).setText("3:21 PM");((TextView)field(activity,"greeting")).setText("Good evening");ValueAnimator asar=(ValueAnimator)field(p,"glowAnimator");asar.setCurrentPlayTime(1800);capture(root,null,"home-asar-"+suffix,w,h);
   activity.showJarvis();Dialog jarvis=org.robolectric.shadows.ShadowDialog.getLatestDialog();capture(root,jarvis,"jarvis-"+suffix,w,h);JarvisRepairTest.find(jarvis.getWindow().getDecorView(),"Speedtest").performClick();JarvisRepairTest.pump();capture(root,jarvis,"speedtest-"+suffix,w,h);jarvis.dismiss();p.destroy();
  }
 }
 @Test public void native1080AndGlowVideoFrames()throws Exception{preview(1920,1080,"1080",true);}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void native720FitsAllFiveTimes()throws Exception{preview(1280,720,"720",false);}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void native4kFitsAllFiveTimes()throws Exception{preview(3840,2160,"4k",false);}
}
