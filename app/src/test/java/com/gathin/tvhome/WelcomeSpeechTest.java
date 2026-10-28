package com.gathin.tvhome;
import android.content.*;import android.os.*;import android.provider.Settings;import android.speech.tts.TextToSpeech;
import java.util.Locale;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowTextToSpeech;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34)
public class WelcomeSpeechTest {
 Context ctx(){return RuntimeEnvironment.getApplication();}
 @Before public void setup(){Settings.Global.putInt(ctx().getContentResolver(),Settings.Global.BOOT_COUNT,41);ShadowTextToSpeech.addLanguageAvailability(Locale.US);}
 TextToSpeech init(WelcomeSpeech w){w.speak(false);TextToSpeech t=ShadowTextToSpeech.getLastTextToSpeechInstance();assertNotNull(t);Shadows.shadowOf(t).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();return t;}
 @Test public void greetingIsExactlyHelloMasterAndUsesEnglish(){WelcomeSpeech w=new WelcomeSpeech(ctx());TextToSpeech t=init(w);assertEquals("Hello, Master.",Shadows.shadowOf(t).getLastSpokenText());assertEquals("en",Shadows.shadowOf(t).getCurrentLanguage().getLanguage());assertFalse(w.pending());w.cancel();}
 @Test public void sameBootDoesNotGreetAgainWhenReturningFromApps(){WelcomeSpeech w=new WelcomeSpeech(ctx());init(w);w.cancel();assertFalse(w.pending());w.speak(false);assertFalse(w.pending());w.cancel();}
 @Test public void activityRecreationDoesNotRepeatGreeting(){WelcomeSpeech w=new WelcomeSpeech(ctx());init(w);w.cancel();WelcomeSpeech second=new WelcomeSpeech(ctx());assertFalse(second.pending());second.cancel();}
 @Test public void rebootMakesGreetingPendingAgain(){WelcomeSpeech w=new WelcomeSpeech(ctx());init(w);w.cancel();Settings.Global.putInt(ctx().getContentResolver(),Settings.Global.BOOT_COUNT,42);assertTrue(new WelcomeSpeech(ctx()).pending());}
 @Test public void observedScreenOffAllowsOneWakeGreeting(){WelcomeSpeech w=new WelcomeSpeech(ctx());init(w);w.screenOff();assertTrue(w.pending());init(w);assertFalse(w.pending());w.cancel();}
 @Test public void disabledGreetingStaysSilent(){WelcomeSpeech w=new WelcomeSpeech(ctx());w.setEnabled(false);assertFalse(w.pending());w.speak(true);assertNull(ShadowTextToSpeech.getLastTextToSpeechInstance());w.cancel();}
 @Test public void cancellationStopsSpeechAndShutsDownEngine(){WelcomeSpeech w=new WelcomeSpeech(ctx());TextToSpeech t=init(w);w.cancel();assertTrue(Shadows.shadowOf(t).isStopped());assertTrue(Shadows.shadowOf(t).isShutdown());}
 @Test public void lateInitializationCannotSpeakAfterLeavingHome(){WelcomeSpeech w=new WelcomeSpeech(ctx());w.speak(false);TextToSpeech t=ShadowTextToSpeech.getLastTextToSpeechInstance();w.cancel();Shadows.shadowOf(t).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();assertNull(Shadows.shadowOf(t).getLastSpokenText());}
 @Test public void failedEngineDoesNotMarkGreetingAsPlayed(){WelcomeSpeech w=new WelcomeSpeech(ctx());w.speak(false);TextToSpeech t=ShadowTextToSpeech.getLastTextToSpeechInstance();Shadows.shadowOf(t).getOnInitListener().onInit(TextToSpeech.ERROR);Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(w.pending());assertNull(Shadows.shadowOf(t).getLastSpokenText());w.cancel();}
 @Test public void manualReplayWorksWithoutReboot(){WelcomeSpeech w=new WelcomeSpeech(ctx());init(w);w.cancel();assertFalse(w.pending());w.speak(true);TextToSpeech t=ShadowTextToSpeech.getLastTextToSpeechInstance();Shadows.shadowOf(t).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("Hello, Master.",Shadows.shadowOf(t).getLastSpokenText());w.cancel();}
 @Test public void dashboardSchedulesWelcomeAndDoesNotRepeatOnResume(){
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
   Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1300));
   TextToSpeech t=ShadowTextToSpeech.getLastTextToSpeechInstance();assertNotNull(t);Shadows.shadowOf(t).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();
   assertEquals("Hello, Master.",Shadows.shadowOf(t).getLastSpokenText());
   c.pause().resume();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1300));assertSame(t,ShadowTextToSpeech.getLastTextToSpeechInstance());
  }
 }
 @Test public void dashboardCancelsPendingWelcomeWhenLeavingImmediately(){
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
   c.pause();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1300));assertNull(ShadowTextToSpeech.getLastTextToSpeechInstance());
  }
 }
 @Test public void dashboardObservedScreenWakeSchedulesANewWelcome(){
  try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
   Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1300));TextToSpeech first=ShadowTextToSpeech.getLastTextToSpeechInstance();Shadows.shadowOf(first).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();
   ctx().sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));Shadows.shadowOf(Looper.getMainLooper()).idle();
   ctx().sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1300));
   TextToSpeech second=ShadowTextToSpeech.getLastTextToSpeechInstance();assertNotSame(first,second);Shadows.shadowOf(second).getOnInitListener().onInit(TextToSpeech.SUCCESS);Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals("Hello, Master.",Shadows.shadowOf(second).getLastSpokenText());
  }
 }
}
