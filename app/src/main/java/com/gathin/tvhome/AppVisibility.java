package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Launcher-only preferences: never disable, uninstall or stop another app. */
final class AppVisibility {
    private static SharedPreferences prefs(Context c) {return c.getSharedPreferences("app-visibility",Context.MODE_PRIVATE);}
    static Set<String> effective(Context c,Collection<String> defaults) {
        Set<String> result=new HashSet<>(defaults);
        result.addAll(prefs(c).getStringSet("hidden",Collections.emptySet()));
        result.removeAll(prefs(c).getStringSet("shown",Collections.emptySet()));
        result.remove(c.getPackageName());
        return result;
    }
    static boolean setHidden(Context c,String pkg,boolean hide) {
        if(pkg==null || pkg.equals(c.getPackageName()) || !pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")) return false;
        Map<String,Boolean> update=new HashMap<>();update.put(pkg,hide);return save(c,update);
    }
    static boolean save(Context c,Map<String,Boolean> choices) {
        SharedPreferences p=prefs(c);
        Set<String> hidden=new HashSet<>(p.getStringSet("hidden",Collections.emptySet()));
        Set<String> shown=new HashSet<>(p.getStringSet("shown",Collections.emptySet()));
        for(Map.Entry<String,Boolean> item:choices.entrySet()) {
            String pkg=item.getKey();if(pkg.equals(c.getPackageName())) continue;
            if(Boolean.TRUE.equals(item.getValue())) {hidden.add(pkg);shown.remove(pkg);}
            else {hidden.remove(pkg);shown.add(pkg);}
        }
        return p.edit().putStringSet("hidden",hidden).putStringSet("shown",shown).commit();
    }
}
