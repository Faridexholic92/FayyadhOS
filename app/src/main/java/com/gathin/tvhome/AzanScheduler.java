package com.gathin.tvhome;

import android.Manifest;import android.app.*;import android.app.job.*;import android.content.*;import android.content.pm.PackageManager;import android.os.Build;
import java.time.*;import java.util.*;

/** One explicit exact alarm at a time; JAKIM cache only, no fabricated or repeated catch-up audio. */
final class AzanScheduler {
 static final int REQUEST=7510,JOB=7511,REFRESH=7515;static final long LATE_LIMIT=90_000;
 static final String ACTION="com.gathin.tvhome.AZAN_DUE";
 static final class Event {final String zone,key;final int index;final long at;Event(String zone,LocalDate date,int index,long at){this.zone=zone;this.index=index;this.at=at;key=zone+"|"+date+"|"+index;}}
 static String zone(Context c){return c.getSharedPreferences("solat",0).getString("zone","");}
 static boolean exactAllowed(Context c){AlarmManager a=c.getSystemService(AlarmManager.class);return a!=null&&(Build.VERSION.SDK_INT<31||a.canScheduleExactAlarms());}
 static boolean notificationsAllowed(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);if(n==null||!n.areNotificationsEnabled())return false;
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
  NotificationChannel channel=n.getNotificationChannel(AzanService.CHANNEL);return channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;
 }
 static String problem(Context c){
  if(!zone(c).matches("[A-Z]{3}[0-9]{2}"))return "Pilih negeri / zon JAKIM dahulu.";
  if(!AzanStore.present(AzanStore.named(c,false)))return "Audio azan umum belum dipilih.";
  if(!exactAllowed(c))return "Benarkan Alarms & reminders dalam tetapan Android.";
  if(!notificationsAllowed(c))return "Benarkan notifikasi azan dalam tetapan Android.";
  return "";
 }
 static Event next(Context c,Instant instant){
  String z=zone(c);if(!z.matches("[A-Z]{3}[0-9]{2}"))return null;
  PrayerRepository repo=new PrayerRepository(c,Clock.fixed(instant,ZoneOffset.UTC));LocalDate today=instant.atZone(PrayerTimes.MALAYSIA).toLocalDate();
  Set<String> played=AzanStore.prefs(c).getStringSet("played",Collections.emptySet());
  for(int offset=0;offset<2;offset++){
   LocalDate date=today.plusDays(offset);PrayerTimes.Day day=repo.cached(z,YearMonth.from(date)).get(date);if(day==null)continue;
   for(int index:PrayerTimes.PRAYERS){long at=date.atTime(day.times[index]).atZone(PrayerTimes.MALAYSIA).toInstant().toEpochMilli();Event e=new Event(z,date,index,at);if(at>instant.toEpochMilli()&&!played.contains(e.key))return e;}
  }return null;
 }
 static Intent intent(Context c){return new Intent(c,AzanReceiver.class).setAction(ACTION);}
 static void cancelAlarm(Context c){AlarmManager a=c.getSystemService(AlarmManager.class);PendingIntent p=PendingIntent.getBroadcast(c,REQUEST,intent(c),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(p!=null){if(a!=null)a.cancel(p);p.cancel();}AzanStore.prefs(c).edit().remove("pending_key").remove("pending_at").remove("pending_zone").remove("pending_index").commit();}
 static synchronized void disable(Context c){AzanStore.prefs(c).edit().putBoolean("enabled",false).commit();cancelAlarm(c);JobScheduler j=c.getSystemService(JobScheduler.class);if(j!=null){j.cancel(JOB);j.cancel(REFRESH);}c.stopService(new Intent(c,AzanService.class));AzanStore.note(c,"Azan automatik dimatikan.");}
 static void refreshJob(Context c){JobScheduler j=c.getSystemService(JobScheduler.class);if(j==null||j.getPendingJob(JOB)!=null)return;
  JobInfo info=new JobInfo.Builder(JOB,new ComponentName(c,AzanRefreshJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(24*60*60_000L).setBackoffCriteria(30*60_000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build();j.schedule(info);
 }
 static void requestRefresh(Context c){JobScheduler j=c.getSystemService(JobScheduler.class);if(j==null||!AzanStore.enabled(c)||!problem(c).isEmpty()||j.getPendingJob(REFRESH)!=null)return;
  j.schedule(new JobInfo.Builder(REFRESH,new ComponentName(c,AzanRefreshJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(30*60_000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());
 }
 static void reconcile(Context c){reconcile(c,Instant.now());}
 static synchronized void reconcile(Context c,Instant now){
  if(!AzanStore.enabled(c)){cancelAlarm(c);JobScheduler j=c.getSystemService(JobScheduler.class);if(j!=null){j.cancel(JOB);j.cancel(REFRESH);}return;}
  String error=problem(c);if(!error.isEmpty()){cancelAlarm(c);AzanStore.note(c,error);JobScheduler j=c.getSystemService(JobScheduler.class);if(j!=null){j.cancel(JOB);j.cancel(REFRESH);}return;}
  refreshJob(c);android.content.SharedPreferences p=AzanStore.prefs(c);long due=p.getLong("pending_at",0);
  // Do not replace an alarm in the brief delivery window when Home resumes at its exact boundary.
  if(due>0&&due<=now.toEpochMilli()&&now.toEpochMilli()-due<=LATE_LIMIT&&zone(c).equals(p.getString("pending_zone",""))&&!p.getStringSet("played",Collections.emptySet()).contains(p.getString("pending_key","")))return;
  Event e=next(c,now);if(e==null){cancelAlarm(c);requestRefresh(c);AzanStore.note(c,"Jadual seterusnya belum tersedia. Muat semula jadual JAKIM.");return;}
  Intent i=intent(c).putExtra("key",e.key).putExtra("at",e.at).putExtra("index",e.index).putExtra("zone",e.zone);
  p.edit().putString("pending_key",e.key).putLong("pending_at",e.at).putString("pending_zone",e.zone).putInt("pending_index",e.index).commit();
  try{PendingIntent pi=PendingIntent.getBroadcast(c,REQUEST,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);c.getSystemService(AlarmManager.class).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,e.at,pi);}
  catch(RuntimeException failed){cancelAlarm(c);AzanStore.note(c,"Alarm tepat tidak dapat dijadualkan. Semak kebenaran Android.");}
 }
 static synchronized boolean take(Context c,Intent i,Instant now){
  if(!AzanStore.enabled(c)||!problem(c).isEmpty())return false;
  var p=AzanStore.prefs(c);String key=i.getStringExtra("key"),z=i.getStringExtra("zone");long at=i.getLongExtra("at",0);int index=i.getIntExtra("index",-1);
  if(key==null||!key.equals(p.getString("pending_key",""))||at!=p.getLong("pending_at",0)||!zone(c).equals(z)||!Arrays.stream(PrayerTimes.PRAYERS).anyMatch(x->x==index))return false;
  long delay=now.toEpochMilli()-at;if(delay<0||delay>LATE_LIMIT){if(delay>LATE_LIMIT){cancelAlarm(c);AzanStore.note(c,"Alarm lewat melebihi 90 saat; azan lama tidak dimainkan semula.");}return false;}
  LocalDate date=Instant.ofEpochMilli(at).atZone(PrayerTimes.MALAYSIA).toLocalDate();PrayerTimes.Day day=new PrayerRepository(c).cached(z,YearMonth.from(date)).get(date);
  if(day==null||!new Event(z,date,index,at).key.equals(key)||date.atTime(day.times[index]).atZone(PrayerTimes.MALAYSIA).toInstant().toEpochMilli()!=at){cancelAlarm(c);AzanStore.note(c,"Jadual berubah / tiada; alarm lama dibatalkan.");return false;}
  Set<String> played=new TreeSet<>(p.getStringSet("played",Collections.emptySet()));if(!played.add(key))return false;while(played.size()>64)played.remove(played.iterator().next());
  p.edit().putStringSet("played",played).remove("pending_key").remove("pending_at").remove("pending_zone").remove("pending_index").commit();return true;
 }
}
