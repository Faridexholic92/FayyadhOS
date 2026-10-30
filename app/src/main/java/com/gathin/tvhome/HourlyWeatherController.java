package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Foreground-only current/+3h/+6h display, explicit coordinate setup and no GPS/geocoding. */
final class HourlyWeatherController {
    interface Source {HourlyWeather cached(double lat,double lon);HourlyWeather fetch(double lat,double lon,long now)throws Exception;}
    static final String PREFS="hourly-weather";
    private final DashboardActivity activity;private final LinearLayout widget;
    private final SharedPreferences prefs;private final Source source;private final Clock clock;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final TextView[] times=new TextView[3],values=new TextView[3];private final ImageView[] icons=new ImageView[3];
    private final TextView attribution;
    private HourlyWeather data;private boolean active,busy,closed,loaded,fromCache;private int generation;
    private String problem="",loadedLocation="";private long nextFetch;
    private final Runnable tick=this::reconcile;
    HourlyWeatherController(DashboardActivity activity){this(activity,new HourlyWeatherRepository(activity),Clock.systemUTC());}
    HourlyWeatherController(DashboardActivity a,Source source,Clock clock){
        activity=a;this.source=source;this.clock=clock;prefs=a.getSharedPreferences(PREFS,0);
        widget=new LinearLayout(a);widget.setOrientation(LinearLayout.VERTICAL);widget.setTag("hourly-weather");widget.setFocusable(false);widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        LinearLayout row=new LinearLayout(a);widget.addView(row,new LinearLayout.LayoutParams(-1,a.d(62)));
        for(int i=0;i<3;i++){
            LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.CENTER);row.addView(column,new LinearLayout.LayoutParams(0,-1,1));
            times[i]=label(10);values[i]=label(12);icons[i]=new ImageView(a);icons[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            column.addView(times[i]);column.addView(icons[i],new LinearLayout.LayoutParams(a.d(22),a.d(22)));column.addView(values[i]);
        }
        attribution=label(8);attribution.setGravity(Gravity.CENTER);widget.addView(attribution);render();
    }
    private TextView label(int size){TextView v=GlassUi.text(activity,"",Math.max(8,size*activity.getResources().getDisplayMetrics().widthPixels/(activity.getResources().getDisplayMetrics().density*960f)),GlassUi.WHITE);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);return v;}
    View view(){return widget;}
    boolean configured(){return prefs.getBoolean("enabled",false)&&prefs.contains("latitude")&&prefs.contains("longitude");}
    private double coordinate(String key){try{return Double.parseDouble(prefs.getString(key,""));}catch(Exception e){return Double.NaN;}}
    void resume(){active=true;reconcile();}
    void pause(){active=false;main.removeCallbacksAndMessages(null);}
    void screen(boolean on){if(on)resume();else pause();}
    private void reconcile(){
        if(closed)return;main.removeCallbacks(tick);
        double lat=coordinate("latitude"),lon=coordinate("longitude");
        if(!configured()||!HourlyWeatherRepository.validCoordinates(lat,lon)){data=null;render();return;}
        String location=HourlyWeatherRepository.location(lat,lon);
        if(!location.equals(loadedLocation)){data=null;loaded=false;}
        render();if(!active)return;
        long now=clock.millis();
        if(!busy&&(!loaded||now>=nextFetch))request(lat,lon,location);
        main.postDelayed(tick,60_000);
    }
    private void request(double lat,double lon,String location){
        busy=true;int token=generation;HourlyWeather prior=data;
        worker.execute(()->{
            HourlyWeather result=prior;String error="";boolean cached=fromCache;long due=clock.millis()+1_800_000;
            try{
                if(result==null){result=source.cached(lat,lon);cached=result!=null;}
                long now=clock.millis(),gate=prefs.getLong("request_not_before",0);
                if(result!=null&&result.fresh(clock.instant()))due=result.fetchedAt+1_800_000;
                else due=now;
                due=Math.max(gate,due);
                if(now>=due){prefs.edit().putLong("request_not_before",now+60_000).apply();try{result=source.fetch(lat,lon,now);cached=false;due=now+1_800_000;}catch(Exception failure){cached=result!=null;error="Cuaca jam gagal dikemas kini";due=now+(failure instanceof HourlyWeatherRepository.Failure?((HourlyWeatherRepository.Failure)failure).retry:900_000);prefs.edit().putLong("request_not_before",due).apply();}}
            }catch(Exception failure){error="Cache cuaca tidak tersedia";due=clock.millis()+900_000;}
            HourlyWeather answer=result;String issue=error;long when=due;boolean cache=cached;
            main.post(()->{busy=false;if(closed)return;if(token==generation&&configured()&&location.equals(HourlyWeatherRepository.location(coordinate("latitude"),coordinate("longitude")))){data=answer;problem=issue;nextFetch=when;loaded=true;loadedLocation=location;fromCache=cache;}if(active)reconcile();});
        });
    }
    private void render(){
        Instant now=clock.instant();boolean fresh=data!=null&&data.fresh(now)&&configured();StringBuilder accessible=new StringBuilder("Open-Meteo. Keadaan semasa berasaskan model, bukan pemerhatian setempat. ");
        for(int i=0;i<3;i++){
            HourlyWeather.Point point=!fresh?null:i==0?data.current(now):data.after(now,i*3);
            String title=i==0?"Sekarang":(i==1?"+3j ":"+6j ")+now.plusSeconds(i*3*3600).atZone(WeatherForecast.ZONE).format(java.time.format.DateTimeFormatter.ofPattern("HH:00"));
            times[i].setText(i==0?"Kini":i==1?"+3j":"+6j");times[i].setContentDescription(title);values[i].setText(point==null?"—":String.format(Locale.getDefault(),"%.0f°C",point.temperature));icons[i].setImageResource(point==null?R.drawable.ic_weather_unknown:HourlyWeather.icon(point.code));
            accessible.append(title).append(": ").append(point==null?"Tidak tersedia":point.temperature+"°C, "+HourlyWeather.description(point.code)).append(". ");
        }
        String text=!configured()?"Cuaca jam: tetapkan lokasi dalam Settings":!fresh?"Open-Meteo · tidak tersedia":"Open-Meteo · model"+(fromCache?" · cache":"");
        attribution.setText(text);widget.setContentDescription(accessible+text+(problem.isEmpty()?"":". "+problem));
    }
    void settings(){
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(activity.d(18),activity.d(8),activity.d(18),0);
        TextView explanation=GlassUi.text(activity,"Lokasi ini berasingan daripada MET (wallpaper/hujan). Koordinat dihantar ke Open-Meteo apabila ON. Tiada GPS atau API key. API percuma untuk kegunaan bukan komersial; semak syarat Open-Meteo untuk edaran komersial.",12,GlassUi.WHITE);box.addView(explanation);
        EditText lat=field(box,"Latitude (−90 hingga 90)","latitude"),lon=field(box,"Longitude (−180 hingga 180)","longitude");
        AlertDialog dialog=activity.show(new AlertDialog.Builder(activity).setTitle("Cuaca Sekarang / +3j / +6j").setView(box).setPositiveButton("Simpan & ON",null).setNeutralButton("OFF",(d,w)->{generation++;prefs.edit().putBoolean("enabled",false).apply();data=null;reconcile();}).setNegativeButton("Batal",null));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            double latitude,longitude;try{latitude=Double.parseDouble(lat.getText().toString().trim());longitude=Double.parseDouble(lon.getText().toString().trim());}catch(Exception invalid){lat.setError("Masukkan koordinat nombor");return;}
            if(!HourlyWeatherRepository.validCoordinates(latitude,longitude)){lat.setError("Koordinat di luar julat");return;}
            generation++;data=null;loaded=false;loadedLocation="";prefs.edit().putString("latitude",Double.toString(latitude)).putString("longitude",Double.toString(longitude)).putBoolean("enabled",true).apply();dialog.dismiss();reconcile();
        });
    }
    private EditText field(LinearLayout parent,String hint,String key){TextView label=GlassUi.text(activity,hint,12,GlassUi.WHITE);parent.addView(label);EditText input=new EditText(activity);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);input.setText(prefs.getString(key,""));parent.addView(input);return input;}
    String status(){return "\n\nHourly display: Open-Meteo, independent of MET rain/wallpaper. Current = modelled conditions, not an on-site observation. +3h/+6h use the corresponding MYT hourly slot. Refresh 30 minutes while Home is active, cache validity 2 hours. "+(configured()?"Configured.":"OFF/unconfigured; set coordinates in Settings.")+(problem.isEmpty()?"":" "+problem);}
    void destroy(){closed=true;active=false;generation++;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
