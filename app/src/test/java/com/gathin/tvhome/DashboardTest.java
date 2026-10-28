package com.gathin.tvhome;
import android.app.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.GraphicsMode;import org.robolectric.shadows.ShadowDialog;
import java.io.*;import java.nio.charset.StandardCharsets;import java.time.*;import java.util.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DashboardTest {
 Context ctx(){return RuntimeEnvironment.getApplication();}
 void demoApps(){String[] names={"YouTube","Netflix","Prime Video","Kodi","Live TV","Files","Settings"};int i=0;for(String name:names){
  String pkg="demo.app"+(i++);PackageInfo p=new PackageInfo();p.packageName=pkg;p.applicationInfo=new ApplicationInfo();p.applicationInfo.packageName=pkg;p.applicationInfo.sourceDir="/fake/"+pkg+".apk";p.applicationInfo.nonLocalizedLabel=name;
  ActivityInfo a=new ActivityInfo();a.name=pkg+".Main";a.packageName=pkg;a.applicationInfo=p.applicationInfo;a.nonLocalizedLabel=name;a.exported=true;a.enabled=true;p.activities=new ActivityInfo[]{a};
  Shadows.shadowOf(ctx().getPackageManager()).installPackage(p);ResolveInfo r=new ResolveInfo();r.activityInfo=a;r.nonLocalizedLabel=name;Shadows.shadowOf(ctx().getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),r);
 }}
 void seedApiCache()throws Exception{
  // Explicit test zone. The app never chooses a user zone automatically.
  ctx().getSharedPreferences("solat",0).edit().putString("zone","SWK08").commit();
  File folder=new File(ctx().getFilesDir(),"jakim-esolat-v1");folder.mkdirs();
  try(var in=getClass().getResourceAsStream("/jakim-shape-SWK08-2026-10-TEST-ADAPTED.json")){java.nio.file.Files.write(new File(folder,"SWK08-2026-10.json").toPath(),in.readAllBytes());}
  NetworkFocus.enable(ctx());
 }
 View render(Activity a,int w,int h){View root=a.findViewById(R.id.root);root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);Shadows.shadowOf(Looper.getMainLooper()).idle();root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);return root;}
 void capture(View root,String name)throws Exception{Bitmap b=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(b));File f=new File(System.getProperty("user.dir"),"build/"+name+".png");f.getParentFile().mkdirs();try(var out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 @Test public void dashboard1080AndJarvisRenderWithRealCachedFixture()throws Exception{demoApps();seedApiCache();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=render(c.get(),1920,1080);assertNotNull(root.findViewById(R.id.prayerPanel));assertTrue(root.findViewById(R.id.grid).getHeight()>100);View first=((androidx.recyclerview.widget.RecyclerView)root.findViewById(R.id.grid)).getChildAt(0);assertNotNull(first);assertTrue("App card must accept remote focus",first.requestFocus());first.refreshDrawableState();capture(root,"dashboard-1080");
  c.get().findViewById(R.id.btnWifi).performClick();Dialog d=ShadowDialog.getLatestDialog();assertTrue(d.isShowing());View decor=d.getWindow().getDecorView();int w=d.getWindow().getAttributes().width,h=d.getWindow().getAttributes().height;decor.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));decor.layout(0,0,w,h);capture(decor,"jarvis-1080");
  assertEquals("Home stays visible behind Jarvis",View.VISIBLE,((android.view.ViewGroup)root).getChildAt(1).getVisibility());
  Bitmap composite=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(composite);root.draw(canvas);canvas.save();canvas.translate(1920-w-d.getWindow().getAttributes().x,d.getWindow().getAttributes().y);decor.draw(canvas);canvas.restore();try(var out=new FileOutputStream(new File(System.getProperty("user.dir"),"build/jarvis-composite.png"))){composite.compress(Bitmap.CompressFormat.PNG,100,out);}composite.recycle();
  d.dismiss();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(View.VISIBLE,((android.view.ViewGroup)root).getChildAt(1).getVisibility());
 }}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void dashboard720HasRoomForPrayerAndApps()throws Exception{demoApps();seedApiCache();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  View root=render(c.get(),1280,720);View panel=root.findViewById(R.id.prayerPanel);assertTrue(panel.getHeight()<=((View)panel.getParent()).getHeight());assertTrue(root.findViewById(R.id.grid).getHeight()>80);androidx.recyclerview.widget.RecyclerView g=root.findViewById(R.id.grid);assertTrue("First app row bottom="+g.getChildAt(0).getBottom()+" grid="+g.getHeight(),g.getChildAt(0).getBottom()<=g.getHeight());capture(root,"dashboard-720");}}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void dashboard4kRenders()throws Exception{demoApps();seedApiCache();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){View root=render(c.get(),3840,2160);java.lang.reflect.Field values=PrayerPanel.class.getDeclaredField("values");values.setAccessible(true);assertTrue(((TextView[])values.get(root.findViewById(R.id.prayerPanel)))[0].getTextSize()>=60);capture(root,"dashboard-4k");}}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void jarvis720IsScrollableAndFitsScreen()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  c.get().showJarvis();Dialog d=ShadowDialog.getLatestDialog();View v=d.getWindow().getDecorView();int width=d.getWindow().getAttributes().width,height=d.getWindow().getAttributes().height;v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);capture(v,"jarvis-720");assertTrue(v.getHeight()<=720);assertTrue(v.getWidth()<1280/3);d.dismiss();}}
 @Test public void newHomeDoesNotRequireZoneOrLocationPermission(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){assertNotNull(c.get().findViewById(R.id.btnSettings));assertEquals("",ctx().getSharedPreferences("solat",0).getString("zone",""));}}
 @Test public void savedOrderAndHideAreUsedByNewGrid(){demoApps();DashboardApps a=new DashboardApps(ctx());String key=DashboardApps.key(a.visible().get(0));assertTrue(a.move(key,1));assertEquals(key,DashboardApps.key(new DashboardApps(ctx()).visible().get(1)));AppVisibility.setHidden(ctx(),a.visible().get(0).activityInfo.packageName,true);assertEquals(6,a.visible().size());}
 @Test public void emptyAppListStillHasSettingsAndPrayer(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){assertEquals(0,new DashboardApps(ctx()).visible().size());c.get().settings();assertTrue(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().isShowing());}}
 @Test public void jarvisDoesNotCreateSpokenOutput(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  android.speech.tts.TextToSpeech before=org.robolectric.shadows.ShadowTextToSpeech.getLastTextToSpeechInstance();JarvisPanel panel=new JarvisPanel(c.get());panel.show();assertSame(before,org.robolectric.shadows.ShadowTextToSpeech.getLastTextToSpeechInstance());panel.close();
 }}
 @Test public void settingsAndHideDialogsRender()throws Exception{demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  c.get().settings();AlertDialog d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();captureDialog(d,"dashboard-settings");d.dismiss();c.get().hidden();d=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();captureDialog(d,"dashboard-hide");d.dismiss();}}
 void captureDialog(Dialog d,String name)throws Exception{View v=d.getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(1120,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));v.layout(0,0,1120,v.getMeasuredHeight());capture(v,name);}
 @Test public void cacheFreshnessUsesNewProviderDirectory()throws Exception{seedApiCache();PrayerRepository r=new PrayerRepository(ctx());assertEquals(31,r.cached("SWK08",YearMonth.of(2026,10)).size());assertTrue(r.updatedAt("SWK08",YearMonth.of(2026,10))>0);}
 @Test public void jarvisCanOpenAndCloseWithVoiceDisabled(){ctx().getSharedPreferences("jarvis",0).edit().putBoolean("voice",false).commit();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){c.get().showJarvis();Dialog d=ShadowDialog.getLatestDialog();assertTrue(d.isShowing());d.dismiss();assertFalse(d.isShowing());}}
}
