package com.gathin.tvhome;
public class OfflineTestApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();getSharedPreferences(HourlyWeatherController.PREFS,0).edit().putBoolean("location_offer_shown",true).commit();getSharedPreferences(DailyQuotes.PREFS,0).edit().putBoolean("enabled",false).commit();}
}
