package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import java.util.*;

/** Persistent visual preference only. No change to weather, prayer, icon pack, network or Dim settings. */
final class GlassOptions {
    static final String PREFS="native-glass";
    static final String[] VALUES={"auto","lite","solid"};
    static String mode(Context c){String value=c.getSharedPreferences(PREFS,0).getString("mode","auto");return Arrays.asList(VALUES).contains(value)?value:"auto";}
    static boolean lowRam(Context c){ActivityManager manager=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);return manager!=null&&manager.isLowRamDevice();}
    static String label(Context c){switch(mode(c)){case "lite":return "Lite";case "solid":return "Solid";default:return "Auto";}}
    static void settings(DashboardActivity a,Runnable changed){
        a.show(new AlertDialog.Builder(a).setTitle("Glass appearance · "+label(a))
            .setSingleChoiceItems(new String[]{"Auto · system blur when supported","Lite · layered glass, no blur","Solid · opaque, less transparency"},Arrays.asList(VALUES).indexOf(mode(a)),(d,i)->{a.getSharedPreferences(PREFS,0).edit().putString("mode",VALUES[i]).apply();d.dismiss();changed.run();})
            .setNeutralButton("About",(d,i)->a.message("Native glass","GlassyUI-inspired Android Views, not React or WebView. Auto requests system window blur on supported Android 12+ devices. Disabled/unsupported blur and low-RAM TVs use the dense layered fallback. Lite never requests blur. Solid removes transparency and sheen. The dock and app tiles use lightweight drawn materials, not live blur. Existing Android reduced-motion settings are respected; no new looping animation."))
            .setNegativeButton("Close",null));
    }
}
