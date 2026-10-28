package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, isolated preference namespace. Preview is deliberately session-only. */
final class RainOptions {
    static final String PREFS="rain-on-glass";
    // Verified live: Ds548 = Papar district. Kinarut is a regional proxy, NOT a local observation.
    static final String HOME_ID="Ds548",HOME_NAME="Kinarut";
    static final String HOME_SCOPE="Kinarut uses the Papar district forecast (Ds548); not a Kinarut-specific or hyperlocal observation.";
    static void initializeHome(SharedPreferences p){
        // One-time weather-only migration; prayer and other feature preferences are never touched.
        if(!p.getBoolean("kinarut_default_v1",false))p.edit().putString("location_id",HOME_ID)
                .putString("location_name",HOME_NAME).putBoolean("kinarut_default_v1",true).apply();
    }
    static void useHome(SharedPreferences p){p.edit().putString("location_id",HOME_ID).putString("location_name",HOME_NAME).apply();}
    final boolean enabled,forecastDriven,moving,lightning;
    final String locationId,locationName;
    final int density,intensity,speed;
    RainOptions(SharedPreferences p){
        enabled=p.getBoolean("enabled",true);forecastDriven=p.getBoolean("forecast",true);moving=p.getBoolean("moving",true);lightning=p.getBoolean("lightning",false);
        String id=p.getString("location_id","");locationId=WeatherForecast.validId(id)?id:HOME_ID;
        locationName=HOME_ID.equals(locationId)?HOME_NAME:p.getString("location_name",locationId);
        density=clamp(p.getInt("density",0));intensity=clamp(p.getInt("intensity",0));speed=clamp(p.getInt("speed",0));
    }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    static int clamp(int v){return Math.max(0,Math.min(2,v));}
    int count(){return new int[]{48,72,96}[density];}
    float opacity(){return new float[]{.48f,.62f,.76f}[intensity];}
    float velocity(){return new float[]{.6f,1f,1.5f}[speed];}
    static boolean isVisualKey(String key){return "lightning".equals(key)||"enabled".equals(key)||"forecast".equals(key)||"moving".equals(key)||"location_id".equals(key)||"location_name".equals(key)||"density".equals(key)||"intensity".equals(key)||"speed".equals(key);}
}
