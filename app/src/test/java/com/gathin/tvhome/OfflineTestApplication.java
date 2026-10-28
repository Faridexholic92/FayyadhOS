package com.gathin.tvhome;
public class OfflineTestApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();getSharedPreferences(DailyQuotes.PREFS,0).edit().putBoolean("enabled",false).commit();}
}
