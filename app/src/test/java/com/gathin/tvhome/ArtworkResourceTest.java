package com.gathin.tvhome;
import android.content.*;
import android.content.pm.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34)
public class ArtworkResourceTest {
 Context context;AppArtwork artwork;ResolveInfo app;
 @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();new DashboardTest().demoApps();app=new DashboardApps(context).visible().get(0);artwork=new AppArtwork(context);set("packResources",context.getResources());set("pack",context.getPackageName());}
 void set(String name,Object value)throws Exception{Field f=AppArtwork.class.getDeclaredField(name);f.setAccessible(true);f.set(artwork,value);}
 @SuppressWarnings("unchecked") <T>T field(String name)throws Exception{Field f=AppArtwork.class.getDeclaredField(name);f.setAccessible(true);return (T)f.get(artwork);}
 @Test public void exactMappingActuallyLoadsDrawableAndCachesIt()throws Exception{Map<String,String> mapping=field("mapping");mapping.put(AppArtwork.component(app.activityInfo.packageName+"/"+app.activityInfo.name),"ic_weather_sunny");assertNotNull(artwork.load(app));assertTrue(artwork.status().contains("exact/alias 1"));assertTrue(artwork.status().contains("native 0"));assertNotNull(artwork.load(app));assertTrue(artwork.status().contains("exact/alias 1"));}
 @Test public void aliasMappingActuallyLoadsPackResource()throws Exception{app.activityInfo.targetActivity=".RealActivity";Map<String,String> mapping=field("mapping");mapping.put(AppArtwork.component(app.activityInfo.packageName+"/.RealActivity"),"ic_weather_cloudy");assertNotNull(artwork.load(app));assertTrue(artwork.status().contains("exact/alias 1"));}
 @Test public void changedActivityUsesSamePackageBannerFallback()throws Exception{Map<String,LinkedHashSet<String>> packages=field("packages");packages.put(app.activityInfo.packageName,new LinkedHashSet<>(Arrays.asList("missing_drawable","ic_weather_sunny")));assertNotNull(artwork.load(app));assertTrue(artwork.status().contains("package fallback 1"));assertTrue(artwork.status().contains("native 0"));}
 @Test public void missingDrawableKeepsNativeArtworkAvailable()throws Exception{Map<String,String> mapping=field("mapping");mapping.put(AppArtwork.component(app.activityInfo.packageName+"/"+app.activityInfo.name),"missing_drawable");assertNotNull(artwork.load(app));assertTrue(artwork.status().contains("native 1"));}
}
