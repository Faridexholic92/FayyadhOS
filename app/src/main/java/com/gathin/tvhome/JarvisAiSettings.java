package com.gathin.tvhome;

import android.app.*;
import android.os.*;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** TV-remote settings. Secret entry is local; no key is printed, prefilled or copied to clipboard. */
final class JarvisAiSettings {
    private final DashboardActivity activity;private final JarvisKeyStore store;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<View> controls=new ArrayList<>();
    private JarvisAiClient client;private Dialog dialog;private EditText entry;private TextView status,result;
    private boolean closed,busy,revealed;private View returnFocus;
    JarvisAiSettings(DashboardActivity a){this(a,new JarvisKeyStore(a));}
    JarvisAiSettings(DashboardActivity a,JarvisKeyStore s){activity=a;store=s;}
    private int dp(float n){return GlassUi.dp(activity,n);}
    private TextView button(LinearLayout box,String label,Runnable action){
        TextView v=GlassUi.text(activity,label,14,GlassUi.WHITE);v.setGravity(Gravity.CENTER);v.setPadding(dp(10),dp(9),dp(10),dp(9));GlassUi.focus(v);v.setOnClickListener(x->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);box.addView(v,p);controls.add(v);return v;
    }
    void show(){
        dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(16),dp(20),dp(16));box.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        TextView title=GlassUi.text(activity,"Jarvis AI Settings",22,GlassUi.CYAN);box.addView(title);
        TextView info=GlassUi.text(activity,"Your own GroqCloud key. Requests go directly to Groq over HTTPS. Connection tests use your account and may incur usage. Native actions do not require AI. No microphone.",13,GlassUi.MUTED);box.addView(info);
        status=GlassUi.text(activity,"",13,GlassUi.WHITE);status.setPadding(0,dp(10),0,dp(8));box.addView(status);
        entry=new EditText(activity);entry.setSingleLine(true);entry.setHint("Enter or replace Groq API key");entry.setTextColor(GlassUi.WHITE);entry.setHintTextColor(GlassUi.MUTED);entry.setTextSize(15);
        entry.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);entry.setTransformationMethod(PasswordTransformationMethod.getInstance());
        entry.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);entry.setSaveEnabled(false);entry.setPrivateImeOptions("nm");entry.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING|android.view.inputmethod.EditorInfo.IME_ACTION_DONE);entry.setContentDescription("Groq API key, hidden by default");box.addView(entry,new LinearLayout.LayoutParams(-1,dp(52)));controls.add(entry);
        TextView reveal=button(box,"Show key",()->{});reveal.setOnClickListener(v->{revealed=!revealed;entry.setTransformationMethod(revealed?null:PasswordTransformationMethod.getInstance());entry.setSelection(entry.length());entry.setContentDescription(revealed?"Groq API key, visible":"Groq API key, hidden");reveal.setText(revealed?"Hide key":"Show key");});
        TextView save=button(box,"Save / replace key",()->{
            if(entry.length()==0){result.setText(R.string.jarvis_direct_enter_key_first);return;}
            try{store.save(entry.getText().toString());clearEntry();refresh();result.setText(R.string.jarvis_direct_key_saved);}
            catch(Exception e){result.setText(JarvisAiClient.safeError(e));}
        });
        button(box,"Choose model from Groq",()->run(false));
        button(box,"Test connection",()->run(true));
        button(box,"Remove saved key",()->{
            if(!store.configured()){result.setText(R.string.jarvis_direct_no_saved_key);return;}
            activity.show(new AlertDialog.Builder(activity).setTitle("Remove saved Groq key?").setMessage("Remove this installation's saved key and test status. This does not revoke the key at Groq.")
                .setPositiveButton("Remove",(d,w)->{try{store.remove();clearEntry();refresh();result.setText(R.string.jarvis_direct_key_removed);}catch(Exception e){result.setText(JarvisAiClient.safeError(e));}}).setNegativeButton("Cancel",null));
        });
        result=GlassUi.text(activity,"Native key protection cannot prevent extraction from a compromised/rooted running device. Keep your key private and revoke it at Groq if exposed.",12,GlassUi.MUTED);result.setPadding(0,dp(10),0,dp(4));box.addView(result);
        button(box,"Close",()->dialog.dismiss());
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.addView(box);dialog.setContentView(scroll);
        dialog.setOnKeyListener((d,k,e)->{if(k!=KeyEvent.KEYCODE_BACK)return false;if(e.getAction()==KeyEvent.ACTION_UP){if(busy){result.setText(R.string.jarvis_direct_cancelled);}dialog.dismiss();}return true;});
        dialog.setOnDismissListener(d->release());dialog.show();GlassWindows.apply(dialog);activity.overlayOpened(dialog);
        Window window=dialog.getWindow();if(window!=null){window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);window.setDimAmount(.35f);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            window.setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-dp(32),dp(600)),Math.min(activity.getResources().getDisplayMetrics().heightPixels-dp(32),dp(510)));}
        entry.setOnEditorActionListener((v,action,event)->{InputMethodManager ime=(InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(entry.getWindowToken(),0);save.requestFocus();return true;});
        refresh();entry.requestFocus();
    }
    private void refresh(){
        String test=store.testedAt()==0?"Not tested":Instant.ofEpochMilli(store.testedAt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM HH:mm"))+" (previous success, not a live status)";
        status.setText(activity.getString(R.string.jarvis_direct_settings_status,store.configured()?"Configured":"Not configured",store.model().isEmpty()?"Not selected":store.model(),test));
    }
    private void clearEntry(){entry.getText().clear();revealed=false;entry.setTransformationMethod(PasswordTransformationMethod.getInstance());
        entry.setContentDescription("Groq API key, hidden");for(View v:controls)if(v instanceof TextView&&"Hide key".contentEquals(((TextView)v).getText()))((TextView)v).setText(R.string.jarvis_direct_show_key);}
    private void run(boolean test){
        if(busy||closed)return;
        if(entry.length()>0){result.setText(R.string.jarvis_direct_unsaved_edits);return;}
        if(!store.configured()){result.setText(JarvisAiClient.message(JarvisAiClient.Code.NO_KEY));return;}
        if(test&&store.model().isEmpty()){result.setText(JarvisAiClient.message(JarvisAiClient.Code.MODEL_REQUIRED));return;}
        busy=true;returnFocus=dialog.getWindow().getDecorView().findFocus();for(View v:controls)v.setEnabled(false);result.setText(test?"Testing a real Groq completion…":"Loading models from your Groq account…");result.setFocusable(true);result.requestFocus();
        client=new JarvisAiClient(store,new JarvisAiClient.DirectTransport());
        worker.submit(()->{
            try{
                List<String> models;if(test){client.test();models=null;}else models=client.models();
                handler.post(()->{if(closed)return;finish();refresh();if(test)result.setText(R.string.jarvis_direct_connection_success);else choose(models);});
            }catch(Exception e){String safe=JarvisAiClient.safeError(e);handler.post(()->{if(closed)return;finish();refresh();result.setText(safe);});}
        });
    }
    private void finish(){busy=false;for(View v:controls)v.setEnabled(true);result.setFocusable(false);if(returnFocus!=null&&returnFocus.isShown())returnFocus.requestFocus();else entry.requestFocus();}
    private void choose(List<String> models){
        result.setText(R.string.jarvis_direct_choose_chat_model);
        activity.show(new AlertDialog.Builder(activity).setTitle("Groq models for your key").setItems(models.toArray(new String[0]),(d,i)->{
            try{store.selectModel(models.get(i));refresh();result.setText(R.string.jarvis_direct_model_selected);}catch(Exception e){result.setText(JarvisAiClient.safeError(e));}
        }).setNegativeButton("Cancel",null));
    }
    private void release(){if(closed)return;closed=true;clearEntry();handler.removeCallbacksAndMessages(null);if(client!=null)client.cancel();worker.shutdownNow();activity.overlayClosed(dialog);}
}
