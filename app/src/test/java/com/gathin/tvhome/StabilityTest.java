package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class StabilityTest {
    private Context context(){return RuntimeEnvironment.getApplication();}
    private Bitmap image(int color){Bitmap bitmap=Bitmap.createBitmap(16,9,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);return bitmap;}
    private WallpaperDissolveView view(){WallpaperDissolveView view=new WallpaperDissolveView(context());view.measure(View.MeasureSpec.makeMeasureSpec(160,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(90,View.MeasureSpec.EXACTLY));view.layout(0,0,160,90);view.hostActive(true);return view;}
    private void idle(long ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
    private JSONObject config(String value)throws Exception {
        File file=File.createTempFile("config-bom-",".json",context().getCacheDir());
        try {
            Files.write(file.toPath(),value.getBytes(StandardCharsets.UTF_8));
            java.lang.reflect.Method read=Config.class.getDeclaredMethod("read",File.class);read.setAccessible(true);
            return (JSONObject)read.invoke(null,file);
        } finally {file.delete();}
    }
    @Test public void bomPrefixedConfigStillParses()throws Exception {assertEquals(5,config("\uFEFF{\"columns\":5}").getInt("columns"));}
    @Test public void ordinaryConfigStillParses()throws Exception {assertEquals(3,config("{\"columns\":3}").getInt("columns"));}
    @Test public void emptyConfigDoesNotCrash()throws Exception {assertNull(config(""));}
    @Test public void genericEnglishAndUsDateOverrideRemainDistinct(){
        Configuration uk=new Configuration(context().getResources().getConfiguration());uk.setLocale(Locale.UK);
        Configuration us=new Configuration(context().getResources().getConfiguration());us.setLocale(Locale.US);
        assertEquals("Settings",context().createConfigurationContext(uk).getString(R.string.settings));
        assertEquals("Settings",context().createConfigurationContext(us).getString(R.string.settings));
        assertEquals("d MMM yyyy",context().createConfigurationContext(uk).getString(R.string.date_format));
        assertEquals("MMM d, yyyy",context().createConfigurationContext(us).getString(R.string.date_format));
    }
    @Test public void returningToIncomingSceneCancelsQueueAndNeverRedecodes(){
        WallpaperDissolveView view=view();AtomicInteger stale=new AtomicInteger(),duplicate=new AtomicInteger();
        try {
            view.request("a",()->image(Color.RED));view.request("b",()->image(Color.BLUE));
            view.request("c",()->{stale.incrementAndGet();return image(Color.GREEN);});
            view.request("b",()->{duplicate.incrementAndGet();return image(Color.BLUE);});
            idle(850);assertEquals("b",view.desiredKey());assertFalse(view.animating());assertEquals(0,stale.get());assertEquals(0,duplicate.get());
        } finally {view.destroy();}
    }
    @Test public void failedDecodeRetainsImageAndAllowsRetry(){
        WallpaperDissolveView view=view();AtomicInteger attempts=new AtomicInteger();
        try {
            view.request("a",()->image(Color.RED));Object previous=view.getDrawable();
            view.request("broken",()->{attempts.incrementAndGet();return null;});
            assertSame(previous,view.getDrawable());assertEquals("a",view.desiredKey());
            view.request("broken",()->{attempts.incrementAndGet();return image(Color.BLUE);});idle(850);
            assertEquals(2,attempts.get());assertEquals("broken",view.desiredKey());assertFalse(view.animating());
        } finally {view.destroy();}
    }
    @Test public void disablingMotionMidFadeFinishesAndDrainsLatest(){
        WallpaperDissolveView view=view();AtomicInteger latest=new AtomicInteger();
        try {
            view.request("a",()->image(Color.RED));view.request("b",()->image(Color.BLUE));
            view.request("c",()->{latest.incrementAndGet();return image(Color.GREEN);});
            RainOptions.prefs(context()).edit().putBoolean("moving",false).commit();idle(70);
            assertFalse(view.animating());assertEquals(1,latest.get());assertEquals("c",view.desiredKey());
        } finally {view.destroy();}
    }
    @Test public void systemAnimationOffMidFadeStopsWithoutExtraFrames(){
        WallpaperDissolveView view=view();
        try {
            view.request("a",()->image(Color.RED));view.request("b",()->image(Color.BLUE));
            Settings.Global.putFloat(context().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,0f);idle(70);assertFalse(view.animating());
        } finally {view.destroy();}
    }
}
