package com.gathin.tvhome;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowDialog;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class JarvisRepairTest {
 private final DashboardTest fixture=new DashboardTest();
 @Before public void noTts(){RuntimeEnvironment.getApplication().getSharedPreferences("jarvis",0).edit().putBoolean("voice",false).commit();}
 static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 static TextView find(View root,String label){if(root instanceof TextView&&label.contentEquals(((TextView)root).getText()))return (TextView)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){TextView v=find(((ViewGroup)root).getChildAt(i),label);if(v!=null)return v;}return null;}
 static void pump(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 static void await(java.util.function.BooleanSupplier done)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!done.getAsBoolean()&&System.nanoTime()<until){Thread.sleep(10);pump();}assertTrue("Async action completed",done.getAsBoolean());}
 View home(DashboardActivity a){return ((ViewGroup)a.findViewById(R.id.root)).getChildAt(1);}
 static class FakeAi extends JarvisAiClient {
  final AtomicInteger calls=new AtomicInteger();final CountDownLatch started=new CountDownLatch(1),finish=new CountDownLatch(1);volatile boolean cancelled;String result="TEST FIXTURE: Wi-Fi link speed is not Internet throughput.";Exception failure;JSONObject sent;
  FakeAi(){super(RuntimeEnvironment.getApplication());}
  @Override String explain(JSONObject context)throws Exception{calls.incrementAndGet();sent=context;started.countDown();finish.await(3,TimeUnit.SECONDS);if(failure!=null)throw failure;return result;}
  @Override void cancel(){cancelled=true;finish.countDown();}
 }
 @Test public void closeButtonKeepsHomeGeometryAndRestoresFocus()throws Exception{fixture.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  View root=fixture.render(c.get(),1920,1080);View settings=c.get().findViewById(R.id.btnSettings);settings.requestFocus();View grid=c.get().findViewById(R.id.grid);int width=grid.getWidth(),left=grid.getLeft();
  c.get().showJarvis();Dialog dialog=ShadowDialog.getLatestDialog();assertEquals(View.VISIBLE,home(c.get()).getVisibility());
  find(dialog.getWindow().getDecorView(),"Close").performClick();pump();fixture.render(c.get(),1920,1080);
  assertFalse(dialog.isShowing());assertEquals(View.VISIBLE,home(c.get()).getVisibility());assertEquals(width,grid.getWidth());assertEquals(left,grid.getLeft());assertTrue(settings.hasFocus());
  fixture.capture(root,"repair-after-close-1080");
 }}
 @Test public void remoteBackDismissesJarvisAndRestoresHome()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);c.get().findViewById(R.id.btnWifi).requestFocus();c.get().showJarvis();Dialog dialog=ShadowDialog.getLatestDialog();
  dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));pump();
  assertFalse(dialog.isShowing());assertEquals(View.VISIBLE,home(c.get()).getVisibility());assertTrue(c.get().findViewById(R.id.btnWifi).hasFocus());
 }}
 @Test public void repeatedCloseReopenDoesNotReserveSpaceOrKeepModalState()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);for(int i=0;i<12;i++){c.get().showJarvis();Dialog d=ShadowDialog.getLatestDialog();assertTrue(d.isShowing());d.dismiss();pump();assertTrue(((java.util.Map<?,?>)field(c.get(),"overlays")).isEmpty());assertEquals(View.VISIBLE,home(c.get()).getVisibility());}
 }}
 @Test public void replacedDialogDismissalDoesNotClearNewOwner()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);AlertDialog first=c.get().show(new AlertDialog.Builder(c.get()).setTitle("First"));AlertDialog second=c.get().show(new AlertDialog.Builder(c.get()).setTitle("Second"));
  first.dismiss();pump();assertEquals(1,((java.util.Map<?,?>)field(c.get(),"overlays")).size());assertSame(second,field(c.get(),"popup"));assertEquals(true,field(c.get().rain,"obscured"));
  second.dismiss();pump();assertTrue(((java.util.Map<?,?>)field(c.get(),"overlays")).isEmpty());assertEquals(false,field(c.get().rain,"obscured"));assertNull(field(c.get(),"popup"));
 }}
 @Test public void dialogCloseCancelAndBackLeaveNoHiddenHome()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);
  for(int mode=0;mode<3;mode++){AlertDialog d=c.get().show(new AlertDialog.Builder(c.get()).setTitle("Dialog").setPositiveButton("Close",null).setNegativeButton("Cancel",null));
   if(mode==0)d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();else if(mode==1)d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();else{d.dispatchKeyEvent(new KeyEvent(0,KeyEvent.KEYCODE_BACK));d.dispatchKeyEvent(new KeyEvent(1,KeyEvent.KEYCODE_BACK));}
   pump();assertFalse(d.isShowing());assertEquals(View.VISIBLE,home(c.get()).getVisibility());assertTrue(((java.util.Map<?,?>)field(c.get(),"overlays")).isEmpty());
  }
 }}
 @Test public void nestedSettingsToJarvisAndOptionsMaintainOwnership()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);c.get().settings();AlertDialog settings=(AlertDialog)ShadowDialog.getLatestDialog();settings.getListView().performItemClick(null,3,3);settings.dismiss();pump();Dialog jarvis=ShadowDialog.getLatestDialog();assertTrue(jarvis.isShowing());assertEquals(1,((java.util.Map<?,?>)field(c.get(),"overlays")).size());
  find(jarvis.getWindow().getDecorView(),"Options").performClick();pump();assertFalse(jarvis.isShowing());Dialog options=ShadowDialog.getLatestDialog();assertTrue(options.isShowing());options.dismiss();pump();assertTrue(((java.util.Map<?,?>)field(c.get(),"overlays")).isEmpty());
 }}
 @Test public void prayerCancelUsesSameFocusRestoration()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);View prayer=c.get().findViewById(R.id.prayerPanel);prayer.requestFocus();prayer.performClick();Dialog d=ShadowDialog.getLatestDialog();assertTrue(d.isShowing());assertEquals(1,((java.util.Map<?,?>)field(c.get(),"overlays")).size());d.cancel();pump();assertTrue(prayer.hasFocus());assertEquals(View.VISIBLE,home(c.get()).getVisibility());
 }}
 @Test public void existingButtonDisplaysReturnedTextAndDoesNotDuplicateRequests()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);FakeAi ai=new FakeAi();JarvisPanel panel=new JarvisPanel(c.get(),ai);panel.show();Dialog dialog=ShadowDialog.getLatestDialog();TextView button=find(dialog.getWindow().getDecorView(),"Explain with AI");button.performClick();assertTrue(ai.started.await(2,TimeUnit.SECONDS));button.performClick();assertEquals(1,ai.calls.get());assertFalse(((TextView)field(panel,"speedButton")).isEnabled());ai.finish.countDown();await(button::isEnabled);
  assertEquals("GroqCloud AI · "+ai.result,((TextView)field(panel,"speed")).getText().toString());assertFalse(ai.sent.has("ssid"));assertFalse(ai.sent.has("estimatedDownloadMbps"));fixture.captureDialog(dialog,"repair-ai-response-FIXTURE");panel.close();
 }}
 @Test public void closingDuringAiCancelsAndReopeningCannotShowStaleResponse()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);FakeAi ai=new FakeAi();JarvisPanel panel=new JarvisPanel(c.get(),ai);panel.show();TextView old=(TextView)field(panel,"speed");find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Explain with AI").performClick();assertTrue(ai.started.await(2,TimeUnit.SECONDS));panel.close();assertTrue(ai.cancelled);assertTrue(((ExecutorService)field(panel,"worker")).awaitTermination(2,TimeUnit.SECONDS));pump();assertFalse(old.getText().toString().contains("TEST FIXTURE"));
  c.get().showJarvis();JarvisPanel fresh=(JarvisPanel)field(c.get(),"jarvis");assertEquals("Internet: not tested",((TextView)field(fresh,"speed")).getText().toString());assertEquals(View.VISIBLE,home(c.get()).getVisibility());ShadowDialog.getLatestDialog().dismiss();
 }}
 @Test public void aiFailureAndTimeoutRestoreButtonsWithoutFabricatingSuccess()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);for(Exception failure:new Exception[]{new JarvisAiClient.Failure(JarvisAiClient.Code.NO_KEY),new java.net.SocketTimeoutException(),new java.io.IOException("SECRET MUST NOT LEAK")}){
   FakeAi ai=new FakeAi();ai.failure=failure;ai.finish.countDown();JarvisPanel panel=new JarvisPanel(c.get(),ai);panel.show();TextView button=find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Explain with AI");button.performClick();await(button::isEnabled);String text=((TextView)field(panel,"speed")).getText().toString();assertTrue(text.startsWith("GroqCloud unavailable"));assertFalse(text.contains("SECRET"));assertTrue(((TextView)field(panel,"speedButton")).isEnabled());panel.close();
  }
 }}
 @Test public void missingConfigurationIsActionableWithoutNetworkCall()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);JarvisPanel panel=new JarvisPanel(c.get());panel.show();TextView button=find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Explain with AI");button.performClick();await(button::isEnabled);assertTrue(((TextView)field(panel,"speed")).getText().toString().contains("No API key configured"));panel.close();
 }}
 @Test public void lifecyclePauseClosesEveryTrackedOverlay()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);AlertDialog one=c.get().show(new AlertDialog.Builder(c.get()).setTitle("One"));AlertDialog two=c.get().show(new AlertDialog.Builder(c.get()).setTitle("Two"));c.pause();pump();assertFalse(one.isShowing());assertFalse(two.isShowing());assertTrue(((java.util.Map<?,?>)field(c.get(),"overlays")).isEmpty());
 }}
 @Test public void closingDuringDownloadCancelsProbeAndWorkerWithoutAiOrStaleUi()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);FakeAi ai=new FakeAi();JarvisSpeedtestTest.Controlled conn=new JarvisSpeedtestTest.Controlled();JarvisPanel panel=new JarvisPanel(c.get(),ai,()->new SpeedProbe(()->conn));panel.show();find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Speedtest").performClick();
  find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"GO").performClick();
  TextView result=(TextView)field(panel,"speed");assertEquals("Measuring download…",result.getText().toString());panel.close();assertEquals(true,field(field(panel,"probe"),"cancelled"));assertTrue(((Future<?>)field(panel,"future")).isCancelled());
  assertTrue(((ExecutorService)field(panel,"worker")).awaitTermination(12,TimeUnit.SECONDS));pump();assertEquals(0,ai.calls.get());assertEquals("Measuring download…",result.getText().toString());assertEquals(View.VISIBLE,home(c.get()).getVisibility());
 }}
 @Test public void nativeWifiSettingsDoesNotCallAi()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);FakeAi ai=new FakeAi();JarvisPanel panel=new JarvisPanel(c.get(),ai);panel.show();find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),"Options").performClick();pump();
  AlertDialog options=(AlertDialog)ShadowDialog.getLatestDialog();options.getListView().performItemClick(null,2,2);options.dismiss();pump();assertEquals(android.provider.Settings.ACTION_WIFI_SETTINGS,Shadows.shadowOf(c.get()).getNextStartedActivity().getAction());assertEquals(0,ai.calls.get());
 }}
 @Test public void longAiResultUsesExistingScrollContainerWithoutEllipsis()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  fixture.render(c.get(),1920,1080);FakeAi ai=new FakeAi();ai.result="TEST FIXTURE, NOT LIVE. "+"Network explanation for TV remote use. ".repeat(35);ai.finish.countDown();JarvisPanel panel=new JarvisPanel(c.get(),ai);panel.show();Dialog dialog=ShadowDialog.getLatestDialog();TextView button=find(dialog.getWindow().getDecorView(),"Explain with AI");button.performClick();await(button::isEnabled);
  View decor=dialog.getWindow().getDecorView();int width=dialog.getWindow().getAttributes().width,height=dialog.getWindow().getAttributes().height;decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));decor.layout(0,0,width,height);
  TextView result=(TextView)field(panel,"speed");assertTrue(result.getText().toString().endsWith(ai.result));assertNull(result.getEllipsize());ScrollView scroll=(ScrollView)field(panel,"scroll");assertTrue(scroll.getChildAt(0).getHeight()>scroll.getHeight());assertTrue(button.isFocusable());panel.close();
 }}

}
