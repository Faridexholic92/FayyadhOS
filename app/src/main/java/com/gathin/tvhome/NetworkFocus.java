package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;

/** Reversible traffic reduction for this launcher ONLY. No system, router, DNS or VPN writes. */
final class NetworkFocus {
    private static SharedPreferences prefs(Context c) {return c.getSharedPreferences("network-focus",Context.MODE_PRIVATE);}
    static boolean enabled(Context c) {return prefs(c).getBoolean("snapshot",false);}
    static boolean automaticPrayer(Context c) {return prefs(c).getBoolean("prayer-auto",true);}
    static boolean automaticPlaces(Context c) {return prefs(c).getBoolean("places-auto",true);}
    static boolean enable(Context c) {
        SharedPreferences p=prefs(c);
        if(enabled(c)) return true; // Repeated taps must never overwrite the original snapshot.
        return p.edit().putBoolean("before-prayer",automaticPrayer(c)).putBoolean("before-places",automaticPlaces(c))
                .putBoolean("prayer-auto",false).putBoolean("places-auto",false).putBoolean("snapshot",true).commit();
    }
    static boolean restore(Context c) {
        SharedPreferences p=prefs(c);
        if(!enabled(c)) return false; // No snapshot: do not invent previous settings.
        if(!p.contains("before-prayer") || !p.contains("before-places")) return false;
        return p.edit().putBoolean("prayer-auto",p.getBoolean("before-prayer",true))
                .putBoolean("places-auto",p.getBoolean("before-places",true))
                .remove("snapshot").remove("before-prayer").remove("before-places").commit();
    }
}
