package com.gathin.tvhome;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;
/** Use an installed English voice, preferring offline speech; never clone a voice. */
final class EnglishVoice {
 static boolean configure(TextToSpeech tts){
  try {
   boolean supported=false;
   for(Locale l:new Locale[]{Locale.US,Locale.UK,Locale.ENGLISH})if(tts.setLanguage(l)>=0){supported=true;break;}
   if(!supported)return false;
   Set<Voice> voices=tts.getVoices();Voice local=null;
   if(voices!=null)for(Voice v:voices)if("en".equals(v.getLocale().getLanguage())&&!v.isNetworkConnectionRequired()){
    local=v;if("US".equals(v.getLocale().getCountry()))break;
   }
   if(local!=null)tts.setVoice(local);
   tts.setSpeechRate(.95f);return true;
  }catch(RuntimeException e){return false;}
 }
}
