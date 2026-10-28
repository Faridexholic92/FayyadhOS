package com.gathin.tvhome;
import android.Manifest;
import android.content.*;
import android.location.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34)
public class DeviceProviderTest {
 Context context;LocationManager manager;org.robolectric.shadows.ShadowLocationManager shadow;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);shadow=Shadows.shadowOf(manager);shadow.setLocationEnabled(true);shadow.setProviderEnabled("network",true);}
 Location fix(){Location l=new Location("network");l.setLatitude(0);l.setLongitude(0);l.setTime(System.currentTimeMillis());l.setAccuracy(1000);return l;}
 static final class Result implements DeviceLocation.Result {Location value;String error;int calls;public void found(Location l){value=l;calls++;}public void failed(String s){error=s;calls++;}}
 @Test public void returnsFreshAndroidNetworkFixAndCopiesIt(){Location fix=fix();shadow.setLastKnownLocation("network",fix);Result r=new Result();new DeviceLocation.AndroidSource(context).request(r);assertNotNull(r.value);assertNotSame(fix,r.value);assertEquals(1,r.calls);assertTrue(shadow.getLocationUpdateListeners().isEmpty());}
 @Test public void disabledSystemLocationProducesClearError(){shadow.setLocationEnabled(false);Result r=new Result();new DeviceLocation.AndroidSource(context).request(r);assertNull(r.value);assertTrue(r.error.contains("OFF"));assertTrue(shadow.getLocationUpdateListeners().isEmpty());}
 @Test public void freshCallbackStopsListenersAndTimeout(){Result r=new Result();DeviceLocation.AndroidSource source=new DeviceLocation.AndroidSource(context);source.request(r);assertFalse(shadow.getLocationUpdateListeners().isEmpty());shadow.simulateLocation("network",fix());Shadows.shadowOf(Looper.getMainLooper()).idle();assertNotNull(r.value);assertTrue(shadow.getLocationUpdateListeners().isEmpty());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(26));assertEquals(1,r.calls);}
 @Test public void timeoutRemovesListenersWithoutInventingCoordinates(){Result r=new Result();new DeviceLocation.AndroidSource(context).request(r);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(26));assertNull(r.value);assertNotNull(r.error);assertEquals(1,r.calls);assertTrue(shadow.getLocationUpdateListeners().isEmpty());}
 @Test public void cancellationSuppressesLateCallbacks(){Result r=new Result();DeviceLocation.AndroidSource source=new DeviceLocation.AndroidSource(context);source.request(r);source.cancel();shadow.simulateLocation("network",fix());Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(26));assertEquals(0,r.calls);assertTrue(shadow.getLocationUpdateListeners().isEmpty());}
 @Test public void absentProvidersIsExplicit(){for(String provider:manager.getAllProviders())shadow.removeProvider(provider);shadow.setLocationEnabled(true);Result r=new Result();new DeviceLocation.AndroidSource(context).request(r);assertNull(r.value);assertNotNull(r.error);assertTrue(r.error.contains("network/GPS"));}
}
