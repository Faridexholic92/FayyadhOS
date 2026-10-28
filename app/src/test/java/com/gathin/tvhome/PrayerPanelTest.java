package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PrayerPanelTest {
    @Test public void homeInflatesPrayerPanelAndCompletesLifecycle() throws Exception {
        Activity setup=Robolectric.buildActivity(Activity.class).setup().get();
        java.io.File folder=setup.getExternalFilesDir(null);
        java.nio.file.Files.write(new java.io.File(folder,"config.json").toPath(),
                "{\"places\":false,\"module_cards\":false}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        setup.finish();
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity home=controller.get();
            View panel=home.findViewById(R.id.prayerPanel);
            assertNotNull(panel); assertTrue(panel instanceof PrayerPanel);
            assertEquals(View.VISIBLE,panel.getVisibility());
            View root=home.findViewById(R.id.root);
            root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));
            root.layout(0,0,1920,1080);
            assertTrue(panel.getWidth()>1000);
            assertEquals(R.id.prayerPanel,home.findViewById(R.id.btnSettings).getNextFocusDownId());
        }
    }
    @Test public void widgetLoadsAllZonesAndRequiresExplicitSelection() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity=controller.get();
            PrayerPanel panel=new PrayerPanel(activity); activity.setContentView(panel);
            Field zones=PrayerPanel.class.getDeclaredField("zones"); zones.setAccessible(true);
            assertEquals(60,((List<?>)zones.get(panel)).size());
            Field heading=PrayerPanel.class.getDeclaredField("heading"); heading.setAccessible(true);
            assertTrue(((TextView)heading.get(panel)).getText().toString().contains("Select your JAKIM zone"));
            assertTrue(panel.isFocusable()); assertTrue(panel.isClickable());
            panel.performClick();
            AlertDialog states=ShadowAlertDialog.getLatestAlertDialog();
            int sarawak=-1;
            for(int i=0;i<states.getListView().getCount();i++)
                if("Sarawak".equals(states.getListView().getItemAtPosition(i))) sarawak=i;
            assertTrue(sarawak>=0);
            states.getListView().performItemClick(null,sarawak,sarawak);
            AlertDialog zoneDialog=ShadowAlertDialog.getLatestAlertDialog();
            zoneDialog.getListView().performItemClick(null,7,7);
            assertEquals("SWK08",activity.getSharedPreferences("solat",Activity.MODE_PRIVATE).getString("zone",""));
            assertTrue(((TextView)heading.get(panel)).getText().toString().contains("Kuching"));
            panel.destroy();
        }
    }
    @Test public void actualAndroidViewRendersScheduleAtTvWidth() throws Exception {
        try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity=controller.get();
            PrayerPanel panel=new PrayerPanel(activity); activity.setContentView(panel);
            LocalDate today=LocalDate.now(PrayerTimes.MALAYSIA);
            Field zone=PrayerPanel.class.getDeclaredField("zone"); zone.setAccessible(true); zone.set(panel,"SWK08");
            Field daysField=PrayerPanel.class.getDeclaredField("days"); daysField.setAccessible(true);
            @SuppressWarnings("unchecked") Map<LocalDate,PrayerTimes.Day> days=(Map<LocalDate,PrayerTimes.Day>)daysField.get(panel);
            LocalTime[] times={LocalTime.of(5,14),LocalTime.of(6,23),LocalTime.of(12,27),LocalTime.of(15,44),LocalTime.of(18,28),LocalTime.of(19,38)};
            days.put(today,new PrayerTimes.Day(today,"1448-05-12",times));
            days.put(today.plusDays(1),new PrayerTimes.Day(today.plusDays(1),"1448-05-13",times));
            Method render=PrayerPanel.class.getDeclaredMethod("render"); render.setAccessible(true); render.invoke(panel);
            int width=1920; panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.AT_MOST));
            panel.layout(0,0,width,panel.getMeasuredHeight());
            assertTrue(panel.getMeasuredHeight()>100); assertTrue(panel.getMeasuredHeight()<300);
            Field values=PrayerPanel.class.getDeclaredField("values"); values.setAccessible(true);
            TextView[] cells=(TextView[])values.get(panel);
            assertEquals("05:14",cells[0].getText().toString()); assertEquals("19:38",cells[5].getText().toString());
            for(TextView cell:cells) assertTrue(cell.getWidth()>cell.getPaint().measureText(cell.getText().toString()));
            Bitmap bitmap=Bitmap.createBitmap(width,panel.getHeight(),Bitmap.Config.ARGB_8888);
            panel.draw(new Canvas(bitmap));
            java.io.File file=new java.io.File(System.getProperty("user.dir"),"build/solat-panel-preview.png");
            file.getParentFile().mkdirs();
            try(var output=new java.io.FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.PNG,100,output); }
            bitmap.recycle(); panel.destroy();
        }
    }
}
