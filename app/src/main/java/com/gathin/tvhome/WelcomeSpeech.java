package com.gathin.tvhome;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

/** One welcome per boot or observed screen-off cycle, not every return from an app. */
final class WelcomeSpeech {
 static final String MESSAGE="Hello, Master.";
 private final Context context;private final android.content.SharedPreferences prefs;
 private final Handler handler=new Handler(Looper.getMainLooper());private TextToSpeech tts;
 private int generation;private boolean attempted;
 WelcomeSpeech(Context c){context=c;prefs=c.getSharedPreferences("welcome",0);}
 boolean enabled(){return prefs.getBoolean("enabled",true);}
 void setEnabled(boolean on){prefs.edit().putBoolean("enabled",on).apply();if(!on)cancel();}
 private String bootToken(){
  try {int n=Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1);if(n>=0)return "boot-"+n;}catch(RuntimeException ignored){}
  // Fallback for firmware that withholds BOOT_COUNT. Tolerate normal clock drift.
  long epoch=System.currentTimeMillis()-SystemClock.elapsedRealtime(),saved=prefs.getLong("boot_epoch",0);
  if(saved==0||Math.abs(epoch-saved)>120000){saved=epoch;prefs.edit().putLong("boot_epoch",saved).apply();}
  return "epoch-"+saved;
 }
 boolean pending(){return enabled()&&(prefs.getBoolean("wake_pending",false)||!bootToken().equals(prefs.getString("spoken_boot","")));}
 void screenOff(){cancel();attempted=false;prefs.edit().putBoolean("wake_pending",true).apply();}
 void speak(boolean force){
  if(!enabled()||(!force&&(!pending()||attempted)))return;
  cancel();attempted=true;final int request=generation;
  try {tts=new TextToSpeech(context,status->handler.post(()->{
   if(request!=generation||tts==null)return;
   if(status!=TextToSpeech.SUCCESS||!EnglishVoice.configure(tts)){
    Toast.makeText(context,"English voice unavailable. Open Jarvis > Options > English voice settings.",Toast.LENGTH_LONG).show();cancel();return;
   }
   int result=tts.speak(MESSAGE,TextToSpeech.QUEUE_FLUSH,null,"fayyadh-welcome");
   if(result==TextToSpeech.SUCCESS)prefs.edit().putString("spoken_boot",bootToken()).putBoolean("wake_pending",false).apply();
   else Toast.makeText(context,"The greeting could not be played. Check your speech engine.",Toast.LENGTH_LONG).show();
   handler.postDelayed(()->{if(request==generation)cancel();},8000);
  }));}catch(RuntimeException e){Toast.makeText(context,"Speech engine unavailable. Open Android voice settings.",Toast.LENGTH_LONG).show();cancel();}
 }
 void cancel(){generation++;handler.removeCallbacksAndMessages(null);if(tts!=null){tts.stop();tts.shutdown();tts=null;}}
}
