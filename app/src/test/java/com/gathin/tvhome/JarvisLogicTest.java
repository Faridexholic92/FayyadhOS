package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
public class JarvisLogicTest {
 @Test public void greetingUses24HourInternallyWithEnglishPeriods(){assertEquals("Good Night",DashboardActivity.greeting(0));assertEquals("Good Morning",DashboardActivity.greeting(5));assertEquals("Good Morning",DashboardActivity.greeting(11));assertEquals("Good Afternoon",DashboardActivity.greeting(12));assertEquals("Good Afternoon",DashboardActivity.greeting(13));assertEquals("Good Evening",DashboardActivity.greeting(14));assertEquals("Good Evening",DashboardActivity.greeting(18));assertEquals("Good Night",DashboardActivity.greeting(19));}
 @Test public void wifiBandIsDerivedFromActualFrequency(){assertEquals("2.4 GHz",NetworkSnapshot.band(2437));assertEquals("5 GHz",NetworkSnapshot.band(5180));assertEquals("6 GHz",NetworkSnapshot.band(5975));assertEquals("Unavailable",NetworkSnapshot.band(-1));}
 @Test public void ssidDoesNotInventMissingNames(){assertEquals("Unavailable",NetworkSnapshot.cleanSsid("<unknown ssid>"));assertEquals("Unavailable",NetworkSnapshot.cleanSsid(null));assertEquals("My WiFi",NetworkSnapshot.cleanSsid("\"My WiFi\""));assertEquals("Unavailable",NetworkSnapshot.cleanSsid("\"\""));}
 @Test public void throughputUsesBytesAndSecondsNotWifiLinkRate(){assertEquals(8.0,SpeedProbe.mbps(1_000_000,1),0.0001);assertThrows(IllegalArgumentException.class,()->SpeedProbe.mbps(0,1));assertThrows(IllegalArgumentException.class,()->SpeedProbe.mbps(100,0));}
 @Test public void missingNetworkReportDoesNotClaimMeasuredSpeed(){NetworkSnapshot s=new NetworkSnapshot();assertTrue(s.speech().contains("not yet validated"));assertFalse(s.speech().contains("500 megabit"));}
}
