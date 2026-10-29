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
    static int motion(Context c){return Math.max(0,Math.min(2,prefs(c).getInt("motion",2)));} // Off / Lite / Full
    static String asset(boolean birthday){return "premium/"+(birthday?"birthday.webp":"world.webp");}
}
