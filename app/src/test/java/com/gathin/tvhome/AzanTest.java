package com.gathin.tvhome;

import android.Manifest;import android.app.*;import android.app.job.*;import android.content.*;import android.media.*;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.shadows.*;import org.robolectric.shadows.util.DataSource;
import java.io.*;import java.nio.file.*;import java.time.*;import java.util.*;

@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
public class AzanTest {
 android.content.Context c;final Instant NOON=Instant.parse("2026-10-25T04:00:00Z");
 String fixture()throws Exception{try(var in=getClass().getResourceAsStream("/jakim-SBH07-2026-10-OFFICIAL-CAPTURE.json")){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
 @Before public void setup()throws Exception{
  c=RuntimeEnvironment.getApplication();Shadows.shadowOf((Application)c).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
  Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);c.getSharedPreferences("solat",0).edit().putString("zone","SBH07").commit();
  new PrayerRepository(c,Clock.fixed(NOON,ZoneOffset.UTC),(z,m)->fixture()).fetch("SBH07",YearMonth.of(2026,10),true);
 }
 void audio()throws Exception{Files.write(AzanStore.named(c,false).toPath(),new byte[]{1,2,3,4});}
 void enable()throws Exception{audio();AzanStore.prefs(c).edit().putBoolean("enabled",true).commit();}
 Intent pending(){var p=AzanStore.prefs(c);return AzanScheduler.intent(c).putExtra("key",p.getString("pending_key","")).putExtra("at",p.getLong("pending_at",0)).putExtra("zone",p.getString("pending_zone","")).putExtra("index",p.getInt("pending_index",-1));}
 @Test public void optInDefaultsOffAndMissingAudioDoesNotSchedule()throws Exception{assertFalse(AzanStore.enabled(c));assertTrue(AzanScheduler.problem(c).contains("Audio"));AzanScheduler.reconcile(c,NOON);assertEquals(0,AzanStore.prefs(c).getLong("pending_at",0));assertTrue(Shadows.shadowOf(c.getSystemService(AlarmManager.class)).getScheduledAlarms().isEmpty());}
 @Test public void schedulesExactlyFivePrayerBoundariesAndNeverSunrise()throws Exception{
  String[] before={"04:51:59","12:01:59","15:20:59","17:58:59","19:09:59"};int[] expected={0,2,3,4,5};
  for(int n=0;n<before.length;n++){Instant now=LocalDateTime.parse("2026-10-25T"+before[n]).atZone(PrayerTimes.MALAYSIA).toInstant();AzanScheduler.Event e=AzanScheduler.next(c,now);assertNotNull(e);assertEquals(expected[n],e.index);assertEquals(now.plusSeconds(1).toEpochMilli(),e.at);}
  assertEquals(2,AzanScheduler.next(c,Instant.parse("2026-10-24T22:00:00Z")).index);
 }
 @Test public void MalaysiaScheduleDoesNotFollowDeviceTimezone(){TimeZone old=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));try{assertEquals(Instant.parse("2026-10-25T04:02:00Z").toEpochMilli(),AzanScheduler.next(c,NOON).at);}finally{TimeZone.setDefault(old);}}
 @Test public void onlyOneAlarmAndPersistedNetworkJobAreScheduled()throws Exception{enable();AzanScheduler.reconcile(c,NOON);AzanScheduler.reconcile(c,NOON);assertEquals(1,Shadows.shadowOf(c.getSystemService(AlarmManager.class)).getScheduledAlarms().size());assertEquals(2,AzanStore.prefs(c).getInt("pending_index",-1));JobInfo job=c.getSystemService(JobScheduler.class).getPendingJob(AzanScheduler.JOB);assertNotNull(job);assertTrue(job.isPersisted());assertTrue(job.isPeriodic());assertEquals(JobInfo.NETWORK_TYPE_ANY,job.getNetworkType());}
 @Test public void missingPermissionPreventsSchedulingWithoutInexactFallback()throws Exception{enable();Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(false);AzanScheduler.reconcile(c,NOON);assertEquals(0,AzanStore.prefs(c).getLong("pending_at",0));assertTrue(AzanScheduler.problem(c).contains("Alarms"));}
 @Test public void notificationDenialAlsoBlocksScheduling()throws Exception{enable();Shadows.shadowOf((Application)c).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);AzanScheduler.reconcile(c,NOON);assertEquals(0,AzanStore.prefs(c).getLong("pending_at",0));assertTrue(AzanScheduler.problem(c).contains("notifikasi"));}
 @Test public void alarmIsClaimedOnceThenNextPrayerScheduled()throws Exception{enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();Instant due=Instant.ofEpochMilli(i.getLongExtra("at",0));assertTrue(AzanScheduler.take(c,i,due));assertFalse(AzanScheduler.take(c,i,due));AzanScheduler.reconcile(c,due);assertEquals(3,AzanStore.prefs(c).getInt("pending_index",-1));}
 @Test public void lateOrEarlyAlarmsNeverPlayOldAudio()throws Exception{enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();Instant due=Instant.ofEpochMilli(i.getLongExtra("at",0));assertFalse(AzanScheduler.take(c,i,due.minusMillis(1)));assertFalse(AzanScheduler.take(c,i,due.plusMillis(90_001)));assertEquals(0,AzanStore.prefs(c).getLong("pending_at",0));}
 @Test public void forgedStaleZoneAndSunrisePayloadsAreRejected()throws Exception{enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();Instant due=Instant.ofEpochMilli(i.getLongExtra("at",0));assertFalse(AzanScheduler.take(c,new Intent(i).putExtra("key","wrong"),due));assertFalse(AzanScheduler.take(c,new Intent(i).putExtra("index",1),due));c.getSharedPreferences("solat",0).edit().putString("zone","SWK08").commit();assertFalse(AzanScheduler.take(c,i,due));}
 @Test public void disablingCancelsAlarmJobAndStopsOnlyOwnService()throws Exception{enable();AzanScheduler.reconcile(c,NOON);AzanScheduler.disable(c);assertFalse(AzanStore.enabled(c));assertTrue(Shadows.shadowOf(c.getSystemService(AlarmManager.class)).getScheduledAlarms().isEmpty());assertNull(c.getSystemService(JobScheduler.class).getPendingJob(AzanScheduler.JOB));}
 @Test public void bootDoesNotStartAudioAndMissingCacheDoesNotInventSchedule()throws Exception{enable();new AzanRescheduleReceiver().onReceive(c,new Intent(Intent.ACTION_BOOT_COMPLETED));assertNull(Shadows.shadowOf((Application)c).getNextStartedService());assertNull(AzanScheduler.next(c,Instant.parse("2027-01-01T00:00:00Z")));AzanScheduler.reconcile(c,Instant.parse("2027-01-01T00:00:00Z"));assertEquals(0,AzanStore.prefs(c).getLong("pending_at",0));}
 @Test public void subuhHasOptionalOwnAudioAndOthersUseGeneral()throws Exception{audio();assertEquals(AzanStore.named(c,false),AzanStore.audio(c,0));Files.write(AzanStore.named(c,true).toPath(),new byte[]{5});assertEquals(AzanStore.named(c,true),AzanStore.audio(c,0));for(int i:new int[]{2,3,4,5})assertEquals(AzanStore.named(c,false),AzanStore.audio(c,i));}
 @Test public void importsUserContentPrivatelyAndEmptyInputCannotReplaceGoodFile()throws Exception{
  Uri uri=Uri.parse("content://azan-fixture/audio");byte[] bytes={4,3,2,1};Shadows.shadowOf(c.getContentResolver()).registerInputStream(uri,new ByteArrayInputStream(bytes));AzanStore.importAudio(c,uri,false);assertArrayEquals(bytes,Files.readAllBytes(AzanStore.named(c,false).toPath()));
  Uri empty=Uri.parse("content://azan-fixture/empty");Shadows.shadowOf(c.getContentResolver()).registerInputStream(empty,new ByteArrayInputStream(new byte[0]));assertThrows(IOException.class,()->AzanStore.importAudio(c,empty,false));assertArrayEquals(bytes,Files.readAllBytes(AzanStore.named(c,false).toPath()));assertFalse(new File(AzanStore.named(c,false).getPath()+".tmp").exists());
 }
 @Test public void oversizedAudioIsRejectedAndOriginalKept()throws Exception{audio();Uri uri=Uri.parse("content://azan-fixture/large");Shadows.shadowOf(c.getContentResolver()).registerInputStream(uri,new ByteArrayInputStream(new byte[(int)AzanStore.MAX_BYTES+1]));assertThrows(IOException.class,()->AzanStore.importAudio(c,uri,false));assertEquals(4,AzanStore.named(c,false).length());}
 @Test public void arbitraryRemoteAndFileUrisAreNotImported(){assertThrows(IOException.class,()->AzanStore.importAudio(c,Uri.parse("https://example.invalid/audio.mp3"),false));assertThrows(IOException.class,()->AzanStore.importAudio(c,Uri.parse("file:///tmp/audio.mp3"),false));}
 @org.robolectric.annotation.Implements(android.bluetooth.BluetoothDevice.class) public static class Sdk34BluetoothShadow {}
 @Test @org.robolectric.annotation.Config(shadows=Sdk34BluetoothShadow.class) public void serviceUsesLocalFixtureWithoutRequestingAudioFocusAndStops()throws Exception{
  audio();String path=AzanStore.named(c,false).getAbsolutePath();ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(path),new ShadowMediaPlayer.MediaInfo(60000,0));AudioManager manager=c.getSystemService(AudioManager.class);
  var controller=Robolectric.buildService(AzanService.class).create();try{
   AzanService service=controller.get();assertEquals(Service.START_NOT_STICKY,service.onStartCommand(new Intent(c,AzanService.class).setAction(AzanService.TEST),0,1));Shadows.shadowOf(Looper.getMainLooper()).idle();
   java.lang.reflect.Field f=AzanService.class.getDeclaredField("player");f.setAccessible(true);MediaPlayer player=(MediaPlayer)f.get(service);assertNotNull(player);assertTrue(player.isPlaying());assertNull(Shadows.shadowOf(manager).getLastAudioFocusRequest());
   assertTrue(AzanStore.prefs(c).getString("status","").contains("sedang dimainkan"));service.onStartCommand(new Intent(c,AzanService.class).setAction(AzanService.STOP),0,2);assertNull(f.get(service));
  }finally{controller.destroy();}
 }
 @Test public void settingsStartsDisabledAndPickerUsesOnlyAudioContent(){try(var a=Robolectric.buildActivity(AzanSettingsActivity.class).setup().visible()){
  assertFalse(AzanStore.enabled(c));View decor=a.get().getWindow().getDecorView();TextView choose=JarvisRepairTest.find(decor,"Pilih audio azan umum (wajib)");assertNotNull(choose);assertTrue(choose.isFocusable());choose.performClick();Intent i=Shadows.shadowOf(a.get()).getNextStartedActivityForResult().intent;assertEquals(Intent.ACTION_OPEN_DOCUMENT,i.getAction());assertEquals("audio/*",i.getType());assertNull(Shadows.shadowOf((Application)c).getNextStartedService());
 }}
 @Test public void mainSettingsExposesAzanWithoutChangingExistingMenuIndexes(){try(var a=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  a.get().settings();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals("Azan · Lima waktu",d.getListView().getItemAtPosition(8));d.getListView().performItemClick(null,8,8);assertEquals(AzanSettingsActivity.class.getName(),Shadows.shadowOf(a.get()).getNextStartedActivity().getComponent().getClassName());
 }}
 @Test public void returningHomeAtBoundaryDoesNotReplaceUndeliveredAlarm()throws Exception{
  enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();Instant due=Instant.ofEpochMilli(i.getLongExtra("at",0));
  AzanScheduler.reconcile(c,due.plusSeconds(30));assertEquals(i.getStringExtra("key"),pending().getStringExtra("key"));assertTrue(AzanScheduler.take(c,i,due.plusSeconds(30)));
 }
 @Test public void exactlyNinetySecondsIsAcceptedButOnlyOnce()throws Exception{
  enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();Instant due=Instant.ofEpochMilli(i.getLongExtra("at",0));assertTrue(AzanScheduler.take(c,i,due.plusSeconds(90)));assertFalse(AzanScheduler.take(c,i,due.plusSeconds(90)));
 }
 @Test public void clockRollbackDoesNotReplayClaimedPrayer()throws Exception{
  enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();assertTrue(AzanScheduler.take(c,i,Instant.ofEpochMilli(i.getLongExtra("at",0))));
  AzanScheduler.reconcile(c,NOON);assertEquals(3,pending().getIntExtra("index",-1));
 }
 @Test public void afterIsyakSchedulesTomorrowSubuhFromCache(){
  Instant after=LocalDateTime.parse("2026-10-25T20:00:00").atZone(PrayerTimes.MALAYSIA).toInstant();
  AzanScheduler.Event e=AzanScheduler.next(c,after);assertNotNull(e);assertEquals(0,e.index);assertTrue(e.key.contains("2026-10-26"));
 }
 @Test public void missingCacheRequestsRecoveryJobWithoutInventingAlarm()throws Exception{
  enable();AzanScheduler.reconcile(c,Instant.parse("2027-01-01T00:00:00Z"));assertNotNull(c.getSystemService(JobScheduler.class).getPendingJob(AzanScheduler.REFRESH));assertEquals(0,pending().getLongExtra("at",0));
  AzanScheduler.disable(c);assertNull(c.getSystemService(JobScheduler.class).getPendingJob(AzanScheduler.REFRESH));
 }
 @Test public void blockedNotificationChannelPreventsAutomaticAudio()throws Exception{
  enable();NotificationManager n=c.getSystemService(NotificationManager.class);n.createNotificationChannel(new NotificationChannel(AzanService.CHANNEL,"Azan",NotificationManager.IMPORTANCE_NONE));
  assertFalse(AzanScheduler.notificationsAllowed(c));AzanScheduler.reconcile(c,NOON);assertEquals(0,pending().getLongExtra("at",0));
 }
 @Test public void cacheRemovalInvalidatesAlreadyScheduledAlarm()throws Exception{
  enable();AzanScheduler.reconcile(c,NOON);Intent i=pending();File file=new File(c.getFilesDir(),"jakim-esolat-v1/SBH07-2026-10.json");assertTrue(file.delete());
  assertFalse(AzanScheduler.take(c,i,Instant.ofEpochMilli(i.getLongExtra("at",0))));assertEquals(0,pending().getLongExtra("at",0));
 }

}
