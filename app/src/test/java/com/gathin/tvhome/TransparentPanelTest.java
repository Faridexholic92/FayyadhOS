package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.lang.reflect.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TransparentPanelTest {
    @Test public void glassSurfacesRespectRequestedTranslucentFill() {
        android.content.Context ctx=org.robolectric.RuntimeEnvironment.getApplication();
        for(boolean focus:new boolean[]{false,true})for(int tint:new int[]{0xff000000,0x9910212e,0xc0245360})
            assertEquals(tint,GlassUi.surface(ctx,tint,focus).getColor().getDefaultColor());
    }
    @Test public void focusHasCyanGlowAndIdleHasDarkGlass() {
        android.content.Context ctx=org.robolectric.RuntimeEnvironment.getApplication();
        android.graphics.drawable.StateListDrawable states=GlassUi.states(ctx);
        states.setState(new int[]{});assertEquals(0xad10212e,((GradientDrawable)states.getCurrent()).getColor().getDefaultColor());
        states.setState(new int[]{android.R.attr.state_focused});assertTrue(states.getCurrent() instanceof android.graphics.drawable.LayerDrawable);
        states.setState(new int[]{android.R.attr.state_pressed});assertEquals(0xd0245360,((GradientDrawable)states.getCurrent()).getColor().getDefaultColor());
    }
    @Test public void dashboardRetainsFullScreenWallpaperAndSeparateForeground() {
        try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()) {
            android.widget.FrameLayout root=c.get().findViewById(R.id.root);
            assertEquals(2,root.getChildCount());assertTrue(root.getChildAt(0) instanceof android.widget.ImageView);
        }
    }

    @Test public void backgroundFillRemainsTransparentEvenWhenFocused() throws Exception {
        try(var c=Robolectric.buildActivity(Activity.class).setup()) {
            PrayerPanel p=new PrayerPanel(c.get());
            Method m=PrayerPanel.class.getDeclaredMethod("panelBackground",boolean.class);m.setAccessible(true);
            for(boolean focused:new boolean[]{false,true}) {
                GradientDrawable b=(GradientDrawable)m.invoke(p,focused);
                assertEquals(Color.TRANSPARENT,b.getColor().getDefaultColor());
            }
            p.destroy();
        }
    }
    @Test public void compactPanelFitsAllSixTimesAt720pAnd1080p() throws Exception {
        try(var c=Robolectric.buildActivity(Activity.class).setup()) {
            PrayerPanel p=new PrayerPanel(c.get());
            Field f=PrayerPanel.class.getDeclaredField("values");f.setAccessible(true);
            TextView[] values=(TextView[])f.get(p);
            for(TextView t:values) t.setText("19:38");
            for(int width:new int[]{1280,1920}) {
                p.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.AT_MOST));
                p.layout(0,0,width,p.getMeasuredHeight());
                assertTrue("Height should be compact",p.getHeight()<=150);
                for(TextView t:values) {
                    assertTrue(t.getWidth()>t.getPaint().measureText("19:38"));
                    assertTrue(t.getShadowRadius()>0);
                }
            }
            p.destroy();
        }
    }
    @Test public void allElevenOfflineWallpaperAssetsDecodeAndPathsAreRestricted() throws Exception {
        try(var c=Robolectric.buildActivity(Activity.class).setup()) {
            assertEquals(11,FayyadhWallpapers.FILES.length);
            assertEquals(11,FayyadhWallpapers.NAMES.length);
            assertFalse(FayyadhWallpapers.contains("../../config.json"));
            for(String name:FayyadhWallpapers.FILES) {
                java.io.File file=FayyadhWallpapers.file(c.get(),name);
                assertTrue(file.isFile());assertTrue(file.length()>10000);
                android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;
                android.graphics.BitmapFactory.decodeFile(file.getPath(),options);
                assertEquals(1920,options.outWidth);assertEquals(1080,options.outHeight);
            }
        }
    }
    @Test public void wallpaperPickerHasElevenChoicesAndSavesSelectionWithoutZone() throws Exception {
        try(var c=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=c.get();
            View panel=activity.findViewById(R.id.prayerPanel);
            assertTrue(panel.performLongClick());
            android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertEquals(11,dialog.getListView().getCount());
            dialog.getListView().performItemClick(null,9,9);
            assertEquals("10-Cosmic-Sunrise.jpg",activity.getSharedPreferences("fayyadh-wallpaper",Activity.MODE_PRIVATE).getString("selected",""));
            assertEquals("",activity.getSharedPreferences("solat",Activity.MODE_PRIVATE).getString("zone",""));
        }
    }

}
