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

/** Foreground-only current conditions. User opt-in Android location or manual coordinates; never IP guessing. */
final class HourlyWeatherController {
    interface Source {HourlyWeather cached(double lat,double lon);HourlyWeather fetch(double lat,double lon,long now)throws Exception;}
    static final String PREFS="hourly-weather";
    private final DashboardActivity activity;private final LinearLayout widget;
    private final DeviceLocation.Source locator;private boolean locating;private int locationGeneration;private String locationProblem="";
    private final SharedPreferences prefs;private final Source source;private final Clock clock;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final TextView title,condition,updated;private final ImageView icon;
    private final TextView attribution;
    private HourlyWeather data;private boolean active,busy,closed,loaded,fromCache;private int generation;
    private String problem="",loadedLocation="";private long nextFetch;
    private final Runnable tick=this::reconcile;
    HourlyWeatherController(DashboardActivity activity){this(activity,new HourlyWeatherRepository(activity),Clock.systemUTC());}
    HourlyWeatherController(DashboardActivity a,Source source,Clock clock){this(a,source,clock,new DeviceLocation.AndroidSource(a));}
    HourlyWeatherController(DashboardActivity a,Source source,Clock clock,DeviceLocation.Source locator){
        activity=a;this.source=source;this.clock=clock;this.locator=locator;prefs=a.getSharedPreferences(PREFS,0);
        widget=new LinearLayout(a);widget.setOrientation(LinearLayout.VERTICAL);widget.setTag("hourly-weather");widget.setFocusable(false);widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        widget.setPadding(a.d(8),a.d(4),a.d(8),a.d(4));
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);widget.addView(row,new LinearLayout.LayoutParams(-1,a.d(58)));
        icon=new ImageView(a);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(icon,new LinearLayout.LayoutParams(a.d(42),a.d(42)));
        LinearLayout lines=new LinearLayout(a);lines.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=a.d(10);row.addView(lines,p);
        title=label(22);title.setTypeface(null,android.graphics.Typeface.BOLD);condition=label(12);lines.addView(title);lines.addView(condition);
        updated=label(10);updated.setTextColor(GlassUi.MUTED);widget.addView(updated);
        attribution=label(9);attribution.setTextColor(GlassUi.MUTED);widget.addView(attribution);render();
    }
    private TextView label(int size){TextView v=GlassUi.text(activity,"",Math.max(8,size*activity.getResources().getDisplayMetrics().widthPixels/(activity.getResources().getDisplayMetrics().density*960f)),GlassUi.WHITE);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);return v;}
    View view(){return widget;}
    boolean configured(){return prefs.getBoolean("enabled",false)&&prefs.contains("latitude")&&prefs.contains("longitude");}
    private double coordinate(String key){try{return Double.parseDouble(prefs.getString(key,""));}catch(Exception e){return Double.NaN;}}
    void resume(){active=true;main.removeCallbacks(offerLocation);reconcile();
        if("auto".equals(prefs.getString("location_mode","")))acquireLocation(false);
        else if(!configured()&&!prefs.getBoolean("location_offer_shown",false))main.postDelayed(offerLocation,1800);
    }
    private final Runnable offerLocation=this::offerIfNeeded;
    private void offerIfNeeded(){if(active&&!closed&&!configured()&&!prefs.getBoolean("location_offer_shown",false)){prefs.edit().putBoolean("location_offer_shown",true).apply();offerAutomaticLocation();}}
    void pause(){active=false;locationGeneration++;locating=false;locator.cancel();main.removeCallbacks(tick);main.removeCallbacks(offerLocation);}
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
            HourlyWeather result=prior;String error="";boolean cached=fromCache;long due=clock.millis()+900_000;
            try{
                if(result==null){result=source.cached(lat,lon);cached=result!=null;}
                long now=clock.millis(),gate=prefs.getLong("request_not_before",0);
                if(result!=null&&result.fresh(clock.instant()))due=result.fetchedAt+900_000;
                else due=now;
                due=Math.max(gate,due);
                if(now>=due){prefs.edit().putLong("request_not_before",now+60_000).apply();try{result=source.fetch(lat,lon,now);cached=false;due=now+900_000;}catch(Exception failure){cached=result!=null;error="Cuaca semasa gagal dikemas kini";due=now+(failure instanceof HourlyWeatherRepository.Failure?((HourlyWeatherRepository.Failure)failure).retry:900_000);prefs.edit().putLong("request_not_before",due).apply();}}
            }catch(Exception failure){error="Cache cuaca tidak tersedia";due=clock.millis()+900_000;}
            HourlyWeather answer=result;String issue=error;long when=due;boolean cache=cached;
            main.post(()->{busy=false;if(closed)return;if(token==generation&&configured()&&location.equals(HourlyWeatherRepository.location(coordinate("latitude"),coordinate("longitude")))){data=answer;problem=issue;nextFetch=when;loaded=true;loadedLocation=location;fromCache=cache;}if(active)reconcile();});
        });
    }
    private void render(){
        Instant now=clock.instant();boolean fresh=data!=null&&data.fresh(now)&&configured();HourlyWeather.Point point=fresh?data.current(now):null;
        String label=prefs.getString("label","").trim();if(label.isEmpty())label="Cuaca semasa";
        // Never label user-entered coordinates as the separately configured MET district.
        title.setText(point==null?"— °C":String.format(Locale.getDefault(),"%.0f°C",point.temperature));
        condition.setText(label+" · "+(point==null?(locating?"Mencari lokasi…":locationProblem.isEmpty()?"Belum tersedia":"Lokasi belum tersedia"):HourlyWeather.description(point.code)));
        icon.setImageResource(point==null?R.drawable.ic_weather_unknown:HourlyWeather.icon(point.code));
        String stamp=point==null?"":Instant.ofEpochSecond(point.time).atZone(WeatherForecast.ZONE).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        updated.setText(!locationProblem.isEmpty()?"Lokasi: "+locationProblem:point==null?"":(fromCache?"Cache · ":"Dikemas kini · ")+stamp+" MYT");
        attribution.setText(point==null?"":"Open-Meteo · keadaan semasa (model)");
        widget.setContentDescription(title.getText()+". "+condition.getText()+". "+updated.getText()+". Open-Meteo. Modelled current conditions, not an on-site sensor."+(problem.isEmpty()?"":" "+problem)+(fromCache?" cache":"")+(locationProblem.isEmpty()?"":" "+locationProblem));
    }
    void settings(){
        activity.show(new AlertDialog.Builder(activity).setTitle("Cuaca · Lokasi")
            .setItems(new String[]{"Detect lokasi · Android permission","Tetapan Location Android","Lokasi manual (pilihan)","Status / cuba lokasi semula","Cuaca OFF"},(d,i)->{
                if(i==0)offerAutomaticLocation();
                else if(i==1)activity.system(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                else if(i==2)manualSettings();
                else if(i==3){if("auto".equals(prefs.getString("location_mode",""))&&DeviceLocation.permitted(activity))acquireLocation(true);else offerAutomaticLocation();}
                else disable();
            }).setNegativeButton("Close",null));
    }
    private void disable(){generation++;locationGeneration++;locator.cancel();locating=false;locationProblem="";main.removeCallbacks(offerLocation);prefs.edit().putBoolean("enabled",false).putString("location_mode","off").putBoolean("location_offer_shown",true).apply();data=null;reconcile();}
    void offerAutomaticLocation(){
        activity.show(new AlertDialog.Builder(activity).setTitle("Gunakan lokasi TV untuk cuaca?")
            .setMessage("Benarkan lokasi Android untuk mengesan kawasan TV. Approximate location mencukupi. Koordinat dihantar ke Open-Meteo untuk cuaca semasa; tiada IP lookup, lokasi latar belakang atau GPS tracking berterusan. Sesetengah TV tiada network/GPS provider walaupun permission dibenarkan.")
            .setPositiveButton("Benarkan & detect",(d,i)->{
                prefs.edit().putString("location_mode","auto").putBoolean("enabled",true).putBoolean("location_offer_shown",true).apply();
                if(DeviceLocation.permitted(activity))acquireLocation(true);
                else{activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},74);}
            }).setNeutralButton("Lokasi manual",(d,i)->manualSettings()).setNegativeButton("Nanti",null));
    }
    void permissionResult(){
        if(!"auto".equals(prefs.getString("location_mode",""))||!prefs.getBoolean("enabled",false))return;
        if(DeviceLocation.permitted(activity)){if(active)acquireLocation(true);}
        else{locationProblem="Permission lokasi ditolak. Benarkan melalui App permissions atau pilih lokasi manual.";render();activity.message("Lokasi cuaca",locationProblem);}
    }
    void acquireLocation(boolean explicit){
        if(closed||!active||locating||!"auto".equals(prefs.getString("location_mode",""))||!prefs.getBoolean("enabled",false))return;
        if(!DeviceLocation.permitted(activity)){locationProblem="Akses lokasi belum dibenarkan. Buka Settings → Cuaca.";render();return;}
        long age=clock.millis()-prefs.getLong("location_fix_at",0);
        if(!explicit&&configured()&&age>=0&&age<60*60*1000L)return;
        long gate=prefs.getLong("location_retry_at",0);if(!explicit&&clock.millis()<gate)return;
        locating=true;locationProblem="";int token=++locationGeneration;render();
        locator.request(new DeviceLocation.Result(){
            public void found(android.location.Location location){
                if(closed||!active||token!=locationGeneration)return;locating=false;
                if(!DeviceLocation.usable(location,clock.millis(),android.os.SystemClock.elapsedRealtimeNanos())){failed("Lokasi tidak sah atau terlalu lama. Cuba semula.");return;}
                generation++;data=null;loaded=false;loadedLocation="";nextFetch=0;locationProblem="";
                prefs.edit().putString("latitude",Double.toString(location.getLatitude())).putString("longitude",Double.toString(location.getLongitude())).putString("label","Lokasi TV").putLong("location_fix_at",clock.millis()).putLong("location_retry_at",0).remove("request_not_before").apply();reconcile();
            }
            public void failed(String reason){if(closed||!active||token!=locationGeneration)return;locating=false;locationProblem=reason;prefs.edit().putLong("location_retry_at",clock.millis()+15*60*1000L).apply();render();if(explicit)activity.message("Lokasi cuaca",reason);}
        });
    }
    private void manualSettings(){
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(activity.d(18),activity.d(8),activity.d(18),0);
        TextView explanation=GlassUi.text(activity,"Lokasi ini berasingan daripada MET (wallpaper/hujan). Koordinat dihantar ke Open-Meteo apabila ON. Mod manual: tiada GPS atau API key. API percuma untuk kegunaan bukan komersial; semak syarat Open-Meteo untuk edaran komersial.",12,GlassUi.WHITE);box.addView(explanation);
        EditText lat=field(box,"Latitude (−90 hingga 90)","latitude"),lon=field(box,"Longitude (−180 hingga 180)","longitude");
        EditText label=new EditText(activity);label.setSingleLine(true);label.setHint("Nama lokasi (pilihan)");label.setText(prefs.getString("label",""));box.addView(label);
        AlertDialog dialog=activity.show(new AlertDialog.Builder(activity).setTitle("Cuaca semasa · Lokasi").setView(box).setPositiveButton("Simpan & ON",null).setNeutralButton("OFF",(d,w)->disable()).setNegativeButton("Batal",null));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            double latitude,longitude;try{latitude=Double.parseDouble(lat.getText().toString().trim());longitude=Double.parseDouble(lon.getText().toString().trim());}catch(Exception invalid){lat.setError("Masukkan koordinat nombor");return;}
            if(!HourlyWeatherRepository.validCoordinates(latitude,longitude)){lat.setError("Koordinat di luar julat");return;}
            generation++;locationGeneration++;locating=false;locator.cancel();locationProblem="";data=null;loaded=false;loadedLocation="";prefs.edit().putString("latitude",Double.toString(latitude)).putString("longitude",Double.toString(longitude)).putBoolean("enabled",true).putString("location_mode","manual").putString("label",label.getText().toString().trim()).apply();dialog.dismiss();reconcile();
        });
    }
    private EditText field(LinearLayout parent,String hint,String key){TextView label=GlassUi.text(activity,hint,12,GlassUi.WHITE);parent.addView(label);EditText input=new EditText(activity);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);input.setText(prefs.getString(key,""));parent.addView(input);return input;}
    String status(){return "\n\nCurrent display: Open-Meteo, independent of MET rain/wallpaper. Modelled current conditions, not a live on-site observation. Refresh every 15 minutes while Home is active. Cached data is explicitly labelled and expires after 2 hours. "+(configured()?"Configured.":"Choose Detect lokasi in Settings for permission-based Android location. No IP guessing; manual coordinates remain optional.")+(problem.isEmpty()?"":" "+problem)+(locationProblem.isEmpty()?"":" "+locationProblem);}
    void destroy(){locator.cancel();locationGeneration++;closed=true;active=false;generation++;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
