package com.gathin.tvhome;
import android.graphics.*;
import android.os.Looper;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.GraphicsMode;
import org.json.*;
import java.lang.reflect.Method;
import java.time.Duration;
import java.nio.file.*;
import static org.junit.Assert.*;
/** Host-side native SOFTWARE Canvas diagnostics only, never TV/GPU benchmark claims. */
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w640dp-h360dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RainPerformanceTest {
 JSONObject sample(View root,Canvas canvas,String name,RainGlassView overlay)throws Exception{
  for(int i=0;i<8;i++)root.draw(canvas);
  // Reflection keeps host-only management APIs out of the Android compile bootclasspath.
  Object bean=Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean").invoke(null);
  Class<?> cpuApi=Class.forName("java.lang.management.ThreadMXBean"),allocationApi=Class.forName("com.sun.management.ThreadMXBean");
  Method cpuRead=cpuApi.getMethod("getCurrentThreadCpuTime"),allocationRead=allocationApi.getMethod("getThreadAllocatedBytes",long.class);
  allocationApi.getMethod("setThreadAllocatedMemoryEnabled",boolean.class).invoke(bean,true);
  long id=Thread.currentThread().getId();int n=60;long[] costs=new long[n];
  long bytes=(Long)allocationRead.invoke(bean,id),cpu=(Long)cpuRead.invoke(bean),wall=System.nanoTime();
  for(int i=0;i<n;i++){root.draw(canvas);costs[i]=overlay.effectVisible()?overlay.lastDrawNanos:0;}
  long wallDelta=System.nanoTime()-wall,cpuDelta=(Long)cpuRead.invoke(bean)-cpu;
  long allocated=(Long)allocationRead.invoke(bean,id)-bytes;
  java.util.Arrays.sort(costs);
  return new JSONObject().put("overlay_ondraw_median_us",costs[n/2]/1000.0).put("overlay_ondraw_p95_us",costs[(int)(n*.95)]/1000.0).put("mode",name).put("draws",n).put("wall_ms_per_software_draw",wallDelta/1e6/n).put("thread_cpu_ms_per_software_draw",cpuDelta/1e6/n).put("java_bytes_per_software_draw_including_framework",allocated/(double)n);
 }
 @Test public void recordBeforeAfterSoftwareCanvasAndInactiveBudget()throws Exception{
  DashboardTest fixture=new DashboardTest();fixture.demoApps();
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
   View root=fixture.render(c.get(),1280,720);Bitmap bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
   JSONArray results=new JSONArray();results.put(sample(root,canvas,"disabled_baseline",c.get().rain.view));
   RainOptions.prefs(c.get()).edit().putBoolean("enabled",true).commit();c.get().rain.preview(true);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(850));
   RainGlassView view=c.get().rain.view;assertTrue(view.effectVisible());results.put(sample(root,canvas,"48_drops_default_preview",view));int spriteBytes=view.spriteBytes();
   for(int i=0;i<120;i++){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50));root.draw(canvas);}
   assertEquals(spriteBytes,view.spriteBytes());assertTrue(spriteBytes<140000);
   RainOptions.prefs(c.get()).edit().putInt("density",2).putBoolean("moving",false).commit();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(850));
   results.put(sample(root,canvas,"96_drops_static_preview",view));assertFalse(view.hasScheduledFrame());
   RainOptions.prefs(c.get()).edit().putBoolean("enabled",false).commit();long count=view.frameCallbacks;Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));assertEquals(count,view.frameCallbacks);assertEquals(0,view.spriteBytes());
   JSONObject report=new JSONObject().put("environment","Robolectric native software Canvas on Linux host, 1280x720; NOT Android TV hardware, GPU or startup measurements")
    .put("measurements",results).put("sprite_bytes_active",spriteBytes).put("sprite_bytes_disabled",view.spriteBytes()).put("inactive_extra_callbacks_over_5s",view.frameCallbacks-count)
    .put("active_drop_limit",RainGlassView.MAX_DROPS).put("moving_drop_limit",RainGlassView.MAX_MOVING).put("moving_streak_limit",RainGlassView.MAX_STREAKS).put("default_interval_ms",50);
   java.nio.file.Path out=Paths.get(System.getProperty("user.dir"),"build/rain-performance.json");Files.createDirectories(out.getParent());Files.write(out,report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));bitmap.recycle();
  }
 }
}
