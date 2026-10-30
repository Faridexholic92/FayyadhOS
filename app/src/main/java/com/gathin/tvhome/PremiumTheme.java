package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/** Isolated presentation settings. Never migrates/deletes prayer, audio, keys or app order. */
final class PremiumTheme {
    static final String PREFS="premium-home-v1", WORLD="papazola", BIRTHDAY="birthday", CLASSIC="classic";
    static final LocalDate BIRTHDAY_DATE=LocalDate.of(2026,10,30);
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    static String selected(Context c){return prefs(c).getString("theme",WORLD);}
    static boolean birthday(Context c,LocalDate day){return birthday(selected(c),prefs(c).getBoolean("birthday_auto",true),day);}
    static boolean birthday(String theme,boolean automatic,LocalDate day){
        // One dated fifth birthday, NOT an annual claim that Fayyadh is always five.
        return BIRTHDAY.equals(theme)||WORLD.equals(theme)&&automatic&&BIRTHDAY_DATE.equals(day);
    }
    static final String POSE="pose", RUN="run";
    /** Hero pose follows the approved dusk mockup; free-run keeps the earlier sprite choreography. */
    static String characters(Context c){
        SharedPreferences p=prefs(c);
        // One-time presentation-only migration: honour the request for a static Home.
        if(!p.getBoolean("static_halilintar_v1",false))p.edit().putString("characters",POSE).putBoolean("static_halilintar_v1",true).apply();
        String v=p.getString("characters",POSE);return RUN.equals(v)?RUN:POSE;
    }
    static int motion(Context c){return Math.max(0,Math.min(2,prefs(c).getInt("motion",2)));} // Off / Lite / Full
    static String asset(boolean birthday){
        // The Birthday wallpaper contains characters baked into the pixels; use the clean world
        // backdrop so the live sprites are drawn only once. Birthday copy remains native UI.
        return "premium/world.webp";
    }
}
