package com.gathin.tvhome;

import android.app.*;
import android.graphics.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import javax.crypto.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class JarvisSettingsTest {
 final DashboardTest helper=new DashboardTest();
 @Before public void silent(){RuntimeEnvironment.getApplication().getSharedPreferences("jarvis",0).edit().putBoolean("voice",false).commit();RuntimeEnvironment.getApplication().getSharedPreferences("welcome",0).edit().putBoolean("enabled",false).commit();}
 EditText input(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=input(((ViewGroup)v).getChildAt(i));if(e!=null)return e;}return null;}
 JarvisKeyStore store(DashboardActivity a)throws Exception{KeyGenerator g=KeyGenerator.getInstance("AES");g.init(256);SecretKey key=g.generateKey();return new JarvisKeyStore(a,()->key);}
 @Test public void secretInputIsMaskedNotSavedAndWindowIsSecure()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  helper.render(c.get(),1920,1080);new JarvisAiSettings(c.get(),store(c.get())).show();Dialog d=ShadowDialog.getLatestDialog();EditText e=input(d.getWindow().getDecorView());assertNotNull(e);assertTrue(e.getTransformationMethod() instanceof PasswordTransformationMethod);assertFalse(e.isSaveEnabled());assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,e.getImportantForAutofill());assertTrue((d.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);assertEquals("",e.getText().toString());
  e.setText(JarvisDirectAiTest.KEY);JarvisRepairTest.find(d.getWindow().getDecorView(),"Show key").performClick();assertNull(e.getTransformationMethod());JarvisRepairTest.find(d.getWindow().getDecorView(),"Hide key").performClick();assertNotNull(e.getTransformationMethod());d.dismiss();JarvisRepairTest.pump();assertEquals("",e.getText().toString());
 }}
 @Test public void savingKeyDoesNotCallNetworkAndClearsTheEditor()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  JarvisKeyStore s=store(c.get());new JarvisAiSettings(c.get(),s).show();Dialog d=ShadowDialog.getLatestDialog();EditText e=input(d.getWindow().getDecorView());e.setText(JarvisDirectAiTest.KEY);JarvisRepairTest.find(d.getWindow().getDecorView(),"Save / replace key").performClick();assertEquals(JarvisDirectAiTest.KEY,s.read());assertEquals("",e.getText().toString());assertEquals(0,s.testedAt());assertEquals("",s.model());d.dismiss();
 }}
 @Test public void savedKeyIsNeverPrefilledAndRemovalNeedsConfirmation()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  JarvisKeyStore s=store(c.get());s.save(JarvisDirectAiTest.KEY);new JarvisAiSettings(c.get(),s).show();Dialog d=ShadowDialog.getLatestDialog();assertEquals("",input(d.getWindow().getDecorView()).getText().toString());JarvisRepairTest.find(d.getWindow().getDecorView(),"Remove saved key").performClick();AlertDialog confirm=(AlertDialog)ShadowDialog.getLatestDialog();assertTrue(s.configured());confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();JarvisRepairTest.pump();assertTrue(s.configured());JarvisRepairTest.find(d.getWindow().getDecorView(),"Remove saved key").performClick();confirm=(AlertDialog)ShadowDialog.getLatestDialog();confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();JarvisRepairTest.pump();assertFalse(s.configured());d.dismiss();
 }}
 @Test public void backDismissalRestoresHomeFocusWithoutGeometryChanges()throws Exception{helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  helper.render(c.get(),1920,1080);View settings=c.get().findViewById(R.id.btnSettings);settings.requestFocus();View grid=c.get().findViewById(R.id.grid);int width=grid.getWidth();new JarvisAiSettings(c.get(),store(c.get())).show();Dialog d=ShadowDialog.getLatestDialog();d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));d.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));JarvisRepairTest.pump();helper.render(c.get(),1920,1080);assertFalse(d.isShowing());assertEquals(width,grid.getWidth());assertTrue(settings.hasFocus());
 }}
 @Test public void everySettingsActionCanReceiveRemoteFocus()throws Exception{try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  new JarvisAiSettings(c.get(),store(c.get())).show();Dialog d=ShadowDialog.getLatestDialog();View decor=d.getWindow().getDecorView();for(String label:new String[]{"Show key","Save / replace key","Choose model from Groq","Test connection","Remove saved key","Close"}){TextView b=JarvisRepairTest.find(decor,label);assertNotNull(label,b);assertTrue(label,b.isFocusable());assertTrue(label,b.requestFocus());assertTrue(label,b.isClickable());}d.dismiss();
 }}
}
