package com.gathin.tvhome;

import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.media.*;import android.os.*;
import java.io.*;

/** Local playback, user-visible FGS. No audio-focus request, transport commands, volume writes or TTS. */
public final class AzanService extends Service {
 static final String CHANNEL="azan-playback-v1",PLAY="com.gathin.tvhome.AZAN_PLAY",TEST="com.gathin.tvhome.AZAN_TEST",STOP="com.gathin.tvhome.AZAN_STOP";
 private static final int NOTIFICATION=7512;private static final long MAX_PLAY_MS=15*60_000L;
 private static volatile boolean playing;
 static boolean playing(){return playing;}
 private static final java.util.Set<Runnable> playbackObservers=new java.util.concurrent.CopyOnWriteArraySet<>();
 static void observePlayback(Runnable observer){playbackObservers.add(observer);}
 static void unobservePlayback(Runnable observer){playbackObservers.remove(observer);}
 private void playbackState(boolean value){if(playing!=value){playing=value;for(Runnable observer:playbackObservers){try{observer.run();}catch(RuntimeException ignored){/* Presentation must never interrupt audio. */}}}}
 private MediaPlayer player;private final Handler handler=new Handler(Looper.getMainLooper());private boolean testing;private String expectedZone;
 private final Runnable prepareTimeout=()->finish("Audio tidak dapat disediakan dalam 15 saat.");
 private final Runnable endTimeout=()->finish("Audio dihentikan selepas had 15 minit.");
 @Override public android.os.IBinder onBind(Intent i){return null;}
 static void channel(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);if(n!=null&&n.getNotificationChannel(CHANNEL)==null){NotificationChannel ch=new NotificationChannel(CHANNEL,"Azan · kawalan audio",NotificationManager.IMPORTANCE_LOW);ch.setSound(null,null);ch.enableVibration(false);n.createNotificationChannel(ch);}}
 private Notification notification(String text){
  channel(this);PendingIntent stop=PendingIntent.getService(this,7513,new Intent(this,AzanService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent settings=PendingIntent.getActivity(this,7514,new Intent(this,AzanSettingsActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_silent_mode_off).setContentTitle(testing?"Uji audio azan":"Azan FayyadhOS").setContentText(text).setContentIntent(settings).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_TRANSPORT).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,"Hentikan",stop).build()).build();
 }
 @Override public int onStartCommand(Intent i,int flags,int startId){
  if(i==null||STOP.equals(i.getAction())){finish("Audio dihentikan.");return START_NOT_STICKY;}
  if(!PLAY.equals(i.getAction())&&!TEST.equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  testing=TEST.equals(i.getAction());expectedZone=i.getStringExtra("zone");
  int index=i.getIntExtra("index",2);File audio=AzanStore.audio(this,index);
  releasePlayer();
  try{
   Notification n=notification("Menyediakan audio…");if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);else startForeground(NOTIFICATION,n);
   if(!AzanScheduler.notificationsAllowed(this)||!AzanStore.present(audio)||!testing&&(!AzanStore.enabled(this)||!AzanScheduler.zone(this).equals(expectedZone))){finish("Azan tidak dimainkan: audio, zon atau kebenaran berubah.");return START_NOT_STICKY;}
   MediaPlayer p=new MediaPlayer();player=p;
   // USAGE_MEDIA respects the device's media routing/volume. Never force another player's focus loss.
   p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   p.setWakeMode(getApplicationContext(),PowerManager.PARTIAL_WAKE_LOCK);p.setLooping(false);p.setDataSource(audio.getAbsolutePath());
   p.setOnPreparedListener(mp->{if(mp!=player)return;handler.removeCallbacks(prepareTimeout);
    if(!testing&&(!AzanStore.enabled(this)||!AzanScheduler.zone(this).equals(expectedZone))){finish("Azan dibatalkan selepas perubahan tetapan.");return;}
    try{mp.start();playbackState(true);AzanStore.note(this,testing?"Ujian audio sedang dimainkan.":"Audio azan sedang dimainkan.");getSystemService(NotificationManager.class).notify(NOTIFICATION,notification(testing?"Ujian audio · Hentikan tersedia":"Audio azan · Hentikan tersedia"));handler.postDelayed(endTimeout,MAX_PLAY_MS);}catch(RuntimeException e){finish("Audio tidak boleh dimainkan pada peranti ini.");}
   });
   p.setOnCompletionListener(mp->{if(mp==player)finish(testing?"Ujian audio selesai.":"Audio azan selesai.");});
   p.setOnErrorListener((mp,what,extra)->{if(mp==player)finish("Audio rosak / format tidak disokong. Pilih fail lain.");return true;});
   handler.postDelayed(prepareTimeout,15_000);p.prepareAsync();
  }catch(IOException|RuntimeException e){finish("Audio tidak dapat dibuka. Pilih semula fail dan uji bunyi.");}
  return START_NOT_STICKY;
 }
 private void releasePlayer(){playbackState(false);handler.removeCallbacksAndMessages(null);MediaPlayer old=player;player=null;if(old!=null){try{old.reset();}catch(RuntimeException ignored){}old.release();}}
 private void finish(String message){AzanStore.note(this,message);releasePlayer();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 @Override public void onDestroy(){releasePlayer();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
