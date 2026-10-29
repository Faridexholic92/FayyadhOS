package com.gathin.tvhome;

import android.content.Intent;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DashboardRedesignTest {
 private final DashboardTest fixture=new DashboardTest();
 // Exercise the real view dispatch and FocusFinder path. Robolectric ShadowActivity
 // stores getCurrentFocus separately and does not run the physical ViewRoot input stage.
 private void key(DashboardActivity activity,int key){
  View root=activity.findViewById(R.id.root);
  boolean consumed=root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));
  if(!consumed){int direction=key==KeyEvent.KEYCODE_DPAD_RIGHT?View.FOCUS_RIGHT:key==KeyEvent.KEYCODE_DPAD_LEFT?View.FOCUS_LEFT:0;
   View current=root.findFocus();if(direction!=0&&current!=null){View next=current.focusSearch(direction);if(next!=null)next.requestFocus();}}
  root.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));
  Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(450));
 }

 private View app(RecyclerView grid,int logical){int n=new DashboardApps(RuntimeEnvironment.getApplication()).visible().size();return grid.findViewHolderForAdapterPosition(LoopCarousel.anchor(logical,n)).itemView;}
 private TextView text(Object object,String field)throws Exception{Field f=object.getClass().getDeclaredField(field);f.setAccessible(true);return (TextView)f.get(object);}
 @Test public void carouselIsHorizontalAndStartsOnFirstInstalledApp(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  View root=fixture.render(c.get(),1920,1080);RecyclerView grid=root.findViewById(R.id.grid);
  assertEquals(RecyclerView.HORIZONTAL,((LinearLayoutManager)grid.getLayoutManager()).getOrientation());
  assertTrue("Initial focus belongs to first app",app(grid,0).hasFocus());
  assertEquals(7*LoopCarousel.REPEATS,grid.getAdapter().getItemCount());
 }}
 @Test public void remoteMovesAcrossCardsAndCenterLaunchesMappedComponent(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);View first=app(grid,0);first.requestFocus();key(c.get(),KeyEvent.KEYCODE_DPAD_RIGHT);
  assertTrue("Right focuses next app",app(grid,1).hasFocus());key(c.get(),KeyEvent.KEYCODE_DPAD_CENTER);
  Intent launched=Shadows.shadowOf(c.get()).getNextStartedActivity();assertNotNull(launched);
  assertEquals(new DashboardApps(c.get()).visible().get(1).activityInfo.packageName,launched.getComponent().getPackageName());
  assertEquals(new DashboardApps(c.get()).visible().get(1).activityInfo.name,launched.getComponent().getClassName());
 }}
 @Test public void enterLaunchesAndUpDownRestoresAppFocus(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);app(grid,2).requestFocus();
  key(c.get(),KeyEvent.KEYCODE_DPAD_UP);assertTrue(c.get().findViewById(R.id.prayerPanel).hasFocus());
  key(c.get(),KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(app(grid,2).hasFocus());
  key(c.get(),KeyEvent.KEYCODE_ENTER);assertNotNull(Shadows.shadowOf(c.get()).getNextStartedActivity());
 }}
 @Test @Config(qualifiers="w640dp-h360dp-land-xhdpi") public void layoutAndSettingsLabelFitAt720p()throws Exception{fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1280,720);TextView settings=c.get().findViewById(R.id.btnSettings);assertEquals(1,settings.getLineCount());
  assertTrue(settings.getWidth()-settings.getCompoundPaddingLeft()-settings.getCompoundPaddingRight()>=settings.getPaint().measureText("Settings"));
  View grid=c.get().findViewById(R.id.grid),prayer=c.get().findViewById(R.id.prayerPanel);assertTrue(prayer.getBottom()<grid.getTop());assertTrue(prayer.getBottom()<=720-25);
  assertTrue(app((RecyclerView)grid,0).hasFocus());
 }}
 @Test public void localeAwareClockIsDynamic()throws Exception{Locale before=Locale.getDefault();Locale.setDefault(Locale.GERMANY);try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  c.get().updateClock();String expected=LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMANY));
  assertEquals(expected,text(c.get(),"clock").getText().toString());assertTrue(text(c.get(),"date").getText().toString().contains(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE",Locale.GERMANY))));
 }finally{Locale.setDefault(before);}}
 @Test public void dashboardDoesNotSelectSabahOrInventPrayerData()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  PrayerPanel panel=c.get().findViewById(R.id.prayerPanel);Field field=PrayerPanel.class.getDeclaredField("values");field.setAccessible(true);
  for(TextView value:(TextView[])field.get(panel))assertEquals("--:--",value.getText().toString());
  assertEquals("NEXT PRAYER · Select zone",text(panel,"heading").getText().toString());assertTrue(text(panel,"subtitle").getText().toString().contains("Press OK"));
  assertEquals("",c.get().getSharedPreferences("solat",0).getString("zone",""));
  fixture.capture(fixture.render(c.get(),1920,1080),"dashboard-unconfigured-1080");
 }}
 @Test public void returningFromAppKeepsPreviouslyFocusedCard(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);String label=app(grid,2).getContentDescription().toString();app(grid,2).requestFocus();
  c.pause().resume();fixture.render(c.get(),1920,1080);assertEquals(label,c.get().findViewById(R.id.root).findFocus().getContentDescription().toString());
 }}
 @Test public void carouselScrollsToInstalledAppsBeyondFirstSeven(){fixture.demoApps();
  // Repeat with distinct package/activity keys, rather than adding fake production shortcuts.
  for(int i=0;i<12;i++){
   android.content.pm.PackageInfo p=new android.content.pm.PackageInfo();p.packageName="extra.app"+i;p.applicationInfo=new android.content.pm.ApplicationInfo();p.applicationInfo.packageName=p.packageName;p.applicationInfo.sourceDir="/test/"+i;p.applicationInfo.nonLocalizedLabel="Extra "+i;
   android.content.pm.ActivityInfo a=new android.content.pm.ActivityInfo();a.name=p.packageName+".Main";a.packageName=p.packageName;a.applicationInfo=p.applicationInfo;a.exported=true;a.enabled=true;a.nonLocalizedLabel="Extra "+i;p.activities=new android.content.pm.ActivityInfo[]{a};
   Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).installPackage(p);android.content.pm.ResolveInfo r=new android.content.pm.ResolveInfo();r.activityInfo=a;
   Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),r);
  }
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);app(grid,0).requestFocus();
   for(int i=0;i<15;i++){key(c.get(),KeyEvent.KEYCODE_DPAD_RIGHT);fixture.render(c.get(),1920,1080);}
   View focus=c.get().findViewById(R.id.root).findFocus();assertTrue(grid.getChildAdapterPosition(focus)>=10);assertTrue(focus.getLeft()>=0);assertTrue(focus.getRight()<=grid.getWidth());
  }
 }
 @Test public void backReturnsToSettingsWithoutFinishingHome(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);c.get().onBackPressed();assertTrue(c.get().findViewById(R.id.btnSettings).hasFocus());assertFalse(c.get().isFinishing());
 }}
 @Test public void reorderUsesHorizontalArrowsAndBackSavesWithoutLaunching(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);String firstKey=DashboardApps.key(new DashboardApps(c.get()).visible().get(0));
  app(grid,0).performLongClick();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getListView().performItemClick(null,0,0);dialog.dismiss();fixture.render(c.get(),1920,1080);
  key(c.get(),KeyEvent.KEYCODE_DPAD_RIGHT);fixture.render(c.get(),1920,1080);assertEquals(firstKey,DashboardApps.key(new DashboardApps(c.get()).visible().get(1)));
  key(c.get(),KeyEvent.KEYCODE_DPAD_DOWN);assertTrue(grid.hasFocus());c.get().onBackPressed();fixture.render(c.get(),1920,1080);
  assertEquals(firstKey,DashboardApps.key(new DashboardApps(c.get()).visible().get(1)));assertNull(Shadows.shadowOf(c.get()).getNextStartedActivity());
 }}
 @Test public void hideFromCardOptionsRemovesOnlySelectedApp(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);String hidden=new DashboardApps(c.get()).visible().get(0).activityInfo.packageName;
  app(grid,0).performLongClick();android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getListView().performItemClick(null,1,1);dialog.dismiss();fixture.render(c.get(),1920,1080);
  assertEquals(6*LoopCarousel.REPEATS,grid.getAdapter().getItemCount());for(var app:new DashboardApps(c.get()).visible())assertNotEquals(hidden,app.activityInfo.packageName);
  assertEquals(7,new DashboardApps(c.get()).all().size());
 }}
 @Test public void savedWallpaperSelectionIsNotOverwritten(){var ctx=RuntimeEnvironment.getApplication();ctx.getSharedPreferences("fayyadh-wallpaper",0).edit().putString("selected",FayyadhWallpapers.FILES[2]).commit();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  assertEquals(FayyadhWallpapers.FILES[2],c.get().getSharedPreferences("fayyadh-wallpaper",0).getString("selected",""));
 }}
 @Test @Config(sdk=26) public void androidEightMinimumSdkCreatesFunctionalCarousel(){fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);RecyclerView grid=c.get().findViewById(R.id.grid);assertEquals(7*LoopCarousel.REPEATS,grid.getAdapter().getItemCount());app(grid,0).requestFocus();key(c.get(),KeyEvent.KEYCODE_DPAD_CENTER);assertNotNull(Shadows.shadowOf(c.get()).getNextStartedActivity());
 }}

}
