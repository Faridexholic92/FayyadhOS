package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** One extra Settings entry; existing menus/navigation are not rebuilt. Remote-friendly lists. */
final class RainSettingsDialog {
    private final DashboardActivity activity;
    private final RainController controller;
    private final SharedPreferences prefs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    RainSettingsDialog(DashboardActivity a,RainController c){activity=a;controller=c;prefs=RainOptions.prefs(a);}
    private String on(boolean value){return activity.getString(value?R.string.rain_on:R.string.rain_off);}
    private void later(Runnable task){handler.post(()->{if(!activity.isFinishing()&&!activity.isDestroyed())task.run();});}
    void show(){
        RainOptions o=new RainOptions(prefs);String[] quality=activity.getResources().getStringArray(R.array.rain_quality_choices);
        String[] speeds=activity.getResources().getStringArray(R.array.rain_speed_choices);
        String[] items={activity.getString(R.string.rain_enabled_label,on(o.enabled)),
                activity.getString(R.string.rain_location_label,o.locationId.isEmpty()?activity.getString(R.string.rain_not_selected):o.locationName+" · "+o.locationId),
                activity.getString(R.string.rain_forecast_label,on(o.forecastDriven)),
                activity.getString(R.string.rain_intensity_label,quality[o.intensity]),activity.getString(R.string.rain_density_label,quality[o.density]),
                activity.getString(R.string.rain_speed_label,speeds[o.speed]),activity.getString(R.string.rain_moving_label,on(o.moving)),
                activity.getString(R.string.rain_preview_label,on(controller.previewing())),activity.getString(R.string.rain_status_label),activity.getString(R.string.rain_refresh_label),"Kilat lembut (pilihan): "+on(o.lightning)};
        activity.show(new AlertDialog.Builder(activity).setTitle(R.string.rain_settings_title).setItems(items,(dialog,i)->{dialog.dismiss();later(()->{
            if(i==0){prefs.edit().putBoolean("enabled",!o.enabled).apply();show();}
            else if(i==1)homeLocation();
            else if(i==2){prefs.edit().putBoolean("forecast",!o.forecastDriven).apply();show();}
            else if(i==3)choice("intensity",R.string.rain_intensity_label,o.intensity,quality);
            else if(i==4)choice("density",R.string.rain_density_label,o.density,quality);
            else if(i==5)choice("speed",R.string.rain_speed_label,o.speed,speeds);
            else if(i==6){prefs.edit().putBoolean("moving",!o.moving).apply();show();}
            else if(i==7){if(!o.enabled)Toast.makeText(activity,R.string.rain_choose_enabled,Toast.LENGTH_LONG).show();else controller.preview(!controller.previewing());}
            else if(i==8)activity.show(new AlertDialog.Builder(activity).setTitle(R.string.rain_status_label).setMessage(controller.status()).setPositiveButton(R.string.rain_close,null));
            else if(i==10)activity.show(new AlertDialog.Builder(activity).setTitle("Kilat lembut · pilihan")
                    .setMessage("Kesan setempat sekali setiap 18–30 saat semasa ramalan ribut petir. Tiada flash putih seluruh skrin atau audio. Biarkan OFF jika sensitif terhadap cahaya. Tetapan animasi sistem OFF mematikan pergerakan/kilat.")
                    .setPositiveButton(o.lightning?"Matikan":"Aktifkan",(d,w)->{prefs.edit().putBoolean("lightning",!o.lightning).apply();later(this::show);}).setNegativeButton("Batal",null));
            else{controller.refresh();Toast.makeText(activity,R.string.rain_refresh_message,Toast.LENGTH_LONG).show();}
        });}).setNegativeButton(R.string.rain_close,null));
    }
    private void choice(String key,int title,int selected,String[] values){
        activity.show(new AlertDialog.Builder(activity).setTitle(activity.getString(title,values[selected]))
                .setSingleChoiceItems(values,selected,(d,i)->{prefs.edit().putInt(key,i).apply();d.dismiss();later(this::show);}).setNegativeButton(R.string.rain_cancel,null));
    }
    private void homeLocation(){
        activity.show(new AlertDialog.Builder(activity).setTitle("Weather location · Kinarut")
                .setMessage(RainOptions.HOME_SCOPE+"\n\nMET Malaysia / data.gov.my supplies district forecasts, not current observations.")
                .setPositiveButton("Use Kinarut / Papar",(d,i)->{RainOptions.useHome(prefs);later(this::show);})
                .setNegativeButton(R.string.rain_close,null));
    }
}
