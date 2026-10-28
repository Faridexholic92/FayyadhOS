package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PersonalisationTest {
    private Context ctx() {return RuntimeEnvironment.getApplication();}
    private void addApp(String pkg,String label,String category) {
        PackageInfo p=new PackageInfo();p.packageName=pkg;
        p.applicationInfo=new ApplicationInfo();p.applicationInfo.packageName=pkg;p.applicationInfo.sourceDir="/fake/"+pkg+".apk";p.applicationInfo.nonLocalizedLabel=label;
        ActivityInfo ai=new ActivityInfo();ai.packageName=pkg;ai.name=pkg+".Main";ai.applicationInfo=p.applicationInfo;ai.nonLocalizedLabel=label;ai.enabled=true;ai.exported=true;
        p.activities=new ActivityInfo[]{ai};Shadows.shadowOf(ctx().getPackageManager()).installPackage(p);
        ResolveInfo ri=new ResolveInfo();ri.activityInfo=ai;ri.nonLocalizedLabel=label;
        Shadows.shadowOf(ctx().getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_MAIN).addCategory(category),ri);
    }
    private AppAdapter adapter() {
        AppAdapter a=new AppAdapter(ctx(),null,4,com.gathin.tvhome.Config.load(ctx()));a.reload(Collections.emptyList());return a;
    }
    @Test public void focusEnableAndRestoreDoNotOverwriteFirstSnapshot() {
        assertTrue(NetworkFocus.automaticPrayer(ctx()));assertTrue(NetworkFocus.automaticPlaces(ctx()));
        assertTrue(NetworkFocus.enable(ctx()));assertTrue(NetworkFocus.enabled(ctx()));
        assertFalse(NetworkFocus.automaticPrayer(ctx()));assertFalse(NetworkFocus.automaticPlaces(ctx()));
        assertTrue(NetworkFocus.enable(ctx()));
        assertTrue(NetworkFocus.restore(ctx()));assertFalse(NetworkFocus.enabled(ctx()));
        assertTrue(NetworkFocus.automaticPrayer(ctx()));assertTrue(NetworkFocus.automaticPlaces(ctx()));
    }
    @Test public void restoreUsesActualPreviousValuesNotAssumedDefaults() {
        ctx().getSharedPreferences("network-focus",0).edit().putBoolean("prayer-auto",false).putBoolean("places-auto",true).commit();
        assertTrue(NetworkFocus.enable(ctx()));assertTrue(NetworkFocus.restore(ctx()));
        assertFalse(NetworkFocus.automaticPrayer(ctx()));assertTrue(NetworkFocus.automaticPlaces(ctx()));
    }
    @Test public void restoreWithoutSnapshotLeavesPreferencesUnchanged() {
        ctx().getSharedPreferences("network-focus",0).edit().putBoolean("prayer-auto",false).commit();
        assertFalse(NetworkFocus.restore(ctx()));assertFalse(NetworkFocus.automaticPrayer(ctx()));
    }
    @Test public void damagedSnapshotDoesNotInventValues() {
        ctx().getSharedPreferences("network-focus",0).edit().putBoolean("snapshot",true).putBoolean("prayer-auto",false).commit();
        assertFalse(NetworkFocus.restore(ctx()));assertFalse(NetworkFocus.automaticPrayer(ctx()));
    }
    @Test public void hideAndUnhideOverrideThemeDefaultsAndPersist() {
        List<String> defaults=Collections.singletonList("demo.one");
        assertTrue(AppVisibility.effective(ctx(),defaults).contains("demo.one"));
        assertTrue(AppVisibility.setHidden(ctx(),"demo.one",false));
        assertFalse(AppVisibility.effective(ctx(),defaults).contains("demo.one"));
        assertTrue(AppVisibility.setHidden(ctx(),"demo.two",true));
        assertTrue(AppVisibility.effective(ctx(),defaults).contains("demo.two"));
        assertEquals(Collections.singletonList("demo.one"),defaults);
    }
    @Test public void launcherItselfCannotBeHiddenAndInvalidPackageRejected() {
        assertFalse(AppVisibility.setHidden(ctx(),ctx().getPackageName(),true));
        assertFalse(AppVisibility.setHidden(ctx(),"../../etc",true));
    }
    @Test public void adapterReloadHonoursHideAndUnhideWithoutUninstall() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.two","Beta",Intent.CATEGORY_LEANBACK_LAUNCHER);
        AppAdapter a=adapter();assertEquals(2,a.getItemCount());
        assertTrue(AppVisibility.setHidden(ctx(),"demo.one",true));assertTrue(a.reload(Collections.emptyList()));assertEquals(1,a.getItemCount());
        assertEquals("demo.two",a.packageAt(0));
        assertTrue(AppVisibility.setHidden(ctx(),"demo.one",false));a.reload(Collections.emptyList());assertEquals(2,a.getItemCount());
        try {assertNotNull(ctx().getPackageManager().getPackageInfo("demo.one",0));} catch(Exception e) {fail("Hide must not uninstall");}
    }
    @Test public void movedOrderSurvivesNewAdapterAndCanReset() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.two","Beta",Intent.CATEGORY_LEANBACK_LAUNCHER);
        AppAdapter a=adapter();assertTrue(a.move(1,0));assertEquals("demo.two",a.packageAt(0));
        AppAdapter restored=adapter();assertEquals("demo.two",restored.packageAt(0));
        restored.resetArrangement();restored.reload(Collections.emptyList());assertEquals("demo.one",restored.packageAt(0));
    }
    @Test public void phoneOnlyLaunchableAppsAreAvailableAndTvEntryIsNotDuplicated() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.one","Alpha",Intent.CATEGORY_LAUNCHER);
        addApp("demo.two","Beta",Intent.CATEGORY_LAUNCHER);
        assertEquals(2,AppCatalog.query(ctx()).size());assertEquals(2,adapter().getItemCount());
    }
    @Test public void cancelledHideManagerDoesNotPersistStagedChoices() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            c.get().showHiddenAppsManager();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();
            layoutDialog(d);d.getListView().performItemClick(null,0,0);
            d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertFalse(AppVisibility.effective(ctx(),Collections.emptyList()).contains("demo.one"));
        }
    }
    @Test public void savedHideManagerAndUnhideWorkFromSettings() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            c.get().showHiddenAppsManager();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();layoutDialog(d);d.getListView().performItemClick(null,0,0);d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(AppVisibility.effective(ctx(),Collections.emptyList()).contains("demo.one"));
            c.get().showHiddenAppsManager();d=ShadowAlertDialog.getLatestAlertDialog();layoutDialog(d);d.getListView().performItemClick(null,0,0);d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertFalse(AppVisibility.effective(ctx(),Collections.emptyList()).contains("demo.one"));
        }
    }
    @Test public void networkDialogRequiresExplicitConfirmationForEnableAndRestore() {
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            NetworkTools.show(c.get());AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(null,1,1);
            d=ShadowAlertDialog.getLatestAlertDialog();assertFalse(NetworkFocus.enabled(ctx()));d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(NetworkFocus.enabled(ctx()));
            NetworkTools.show(c.get());d=ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(null,1,1);d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(NetworkFocus.enabled(ctx()));
            NetworkTools.show(c.get());d=ShadowAlertDialog.getLatestAlertDialog();d.getListView().performItemClick(null,2,2);d=ShadowAlertDialog.getLatestAlertDialog();d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertFalse(NetworkFocus.enabled(ctx()));
        }
    }
    @Test public void prayerAutomaticRefreshIsSkippedButManualRefreshAvailable() throws Exception {
        try(var c=Robolectric.buildActivity(Activity.class).setup()) {
            PrayerPanel p=new PrayerPanel(c.get());
            java.lang.reflect.Field zone=PrayerPanel.class.getDeclaredField("zone");zone.setAccessible(true);zone.set(p,"SWK08");
            NetworkFocus.enable(ctx());p.start();
            java.lang.reflect.Field busy=PrayerPanel.class.getDeclaredField("busy");busy.setAccessible(true);assertFalse(busy.getBoolean(p));
            java.lang.reflect.Method options=PrayerPanel.class.getDeclaredMethod("showOptions");
            options.setAccessible(true);options.invoke(p);
            AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
            assertEquals("Refresh prayer times",dialog.getListView().getItemAtPosition(1));
            dialog.dismiss();p.destroy();
        }
    }
    @Test public void hiddenAppOrderIsPreservedWhenVisibleAppsMove() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.two","Beta",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.three","Gamma",Intent.CATEGORY_LEANBACK_LAUNCHER);
        AppAdapter a=adapter();assertTrue(a.move(2,0));
        AppVisibility.setHidden(ctx(),"demo.one",true);a.reload(Collections.emptyList());
        assertTrue(a.move(1,0));
        AppVisibility.setHidden(ctx(),"demo.one",false);
        AppAdapter restored=adapter();
        assertEquals("demo.two",restored.packageAt(0));
        assertEquals("demo.one",restored.packageAt(1));
        assertEquals("demo.three",restored.packageAt(2));
    }
    @Test public void allAppsHiddenStillAllowsOpeningSettingsAndUnhide() {
        addApp("demo.one","Alpha",Intent.CATEGORY_LEANBACK_LAUNCHER);
        AppVisibility.setHidden(ctx(),"demo.one",true);
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(0,adapter().getItemCount());
            c.get().findViewById(R.id.btnSettings).performClick();
            AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();
            assertEquals("Hide / unhide apps",d.getListView().getItemAtPosition(1));
            d.dismiss();c.get().showHiddenAppsManager();
            d=ShadowAlertDialog.getLatestAlertDialog();layoutDialog(d);
            assertTrue(d.getListView().isItemChecked(0));
            d.getListView().performItemClick(null,0,0);
            d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertFalse(AppVisibility.effective(ctx(),Collections.emptyList()).contains("demo.one"));
        }
    }
    @Test public void customizationAndNetworkScreensRenderAtTvSize() throws Exception {
        addApp("demo.one","YouTube",Intent.CATEGORY_LEANBACK_LAUNCHER);
        addApp("demo.two","Netflix",Intent.CATEGORY_LEANBACK_LAUNCHER);
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            c.get().showCustomizationMenu();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(7,d.getListView().getCount());capture(d,"personal-menu");d.dismiss();
            c.get().showHiddenAppsManager();d=ShadowAlertDialog.getLatestAlertDialog();capture(d,"hide-apps");d.dismiss();
            NetworkTools.show(c.get());d=ShadowAlertDialog.getLatestAlertDialog();capture(d,"network-menu");
            d.getListView().performItemClick(null,1,1);d=ShadowAlertDialog.getLatestAlertDialog();capture(d,"boost-confirmation");d.dismiss();
        }
    }
    private void layoutDialog(Dialog dialog) {
        View root=dialog.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(1120,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));
        root.layout(0,0,1120,root.getMeasuredHeight());
    }
    private void capture(Dialog dialog,String name) throws Exception {
        View root=dialog.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(1120,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));
        root.layout(0,0,1120,root.getMeasuredHeight());assertTrue(root.getHeight()<=1000);
        Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
        java.io.File f=new java.io.File(System.getProperty("user.dir"),"build/"+name+"-preview.png");f.getParentFile().mkdirs();
        try(var output=new java.io.FileOutputStream(f)) {bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
    }
}
