package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.widget.FrameLayout;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Foreground-only weather coordinator. Weather scheduling is independent of animation. */
final class RainController {
    interface Source {
        WeatherForecast cached(String id);
        WeatherForecast fetch(String id,long now)throws Exception;
        default String cacheProblem(){return "";}
        default String diagnostic(){return "";}
    }
    static final long REFRESH=6*60*60*1000L,OFFLINE_RETRY=30*60*1000L,FAILURE_RETRY=15*60*1000L;
    final RainGlassView view;
    private final DashboardActivity activity;
    private final FrameLayout root;
    private final SharedPreferences prefs;
    private final Source source;
    private final Clock clock;
    private final BooleanSupplier network;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
    private final Runnable wake=()->{weatherScheduled=false;loaded=false;reconcile();};
    private final View.OnLayoutChangeListener resize=(v,l,t,r,b,ol,ot,or,ob)->resize();
    private final SharedPreferences.OnSharedPreferenceChangeListener changes=(p,key)->{if(RainOptions.isVisualKey(key))settingsChanged();};
    private RainOptions options;
    private WeatherForecast forecast;
    // Data provenance is independent of transport errors: a valid disk cache may have no error.
    private boolean fromCache;
    private WeatherWidget weather;
    void attachWeather(WeatherWidget widget){weather=widget;reconcile();}
    private String weatherId(){return options.locationId;}
    private WeatherForecast.State state=WeatherForecast.State.UNKNOWN;
    private String loadedId="",problem="";
    private long nextFetch;
    private int generation;
    private boolean foreground,interactive=true,obscured,closed,loaded,busy,preview,force,weatherScheduled;

    RainController(DashboardActivity activity,FrameLayout root){this(activity,root,new MetWeatherRepository(activity),Clock.systemUTC(),()->online(activity));}
    RainController(DashboardActivity a,FrameLayout root,Source source,Clock clock,BooleanSupplier network){
        activity=a;this.root=root;this.source=source;this.clock=clock;this.network=network;prefs=RainOptions.prefs(a);RainOptions.initializeHome(prefs);options=new RainOptions(prefs);
        worker.allowCoreThreadTimeOut(true);
        view=new RainGlassView(a);view.setTag("rain-on-glass");
        // Public ViewGroupOverlay: above wallpaper and UI, absent from focus/touch dispatch.
        root.getOverlay().add(view);root.addOnLayoutChangeListener(resize);resize();prefs.registerOnSharedPreferenceChangeListener(changes);
    }
    private void resize(){view.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(root.getHeight(),View.MeasureSpec.EXACTLY));view.layout(0,0,root.getWidth(),root.getHeight());}
    private static boolean online(Context context){
        try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities c=cm.getNetworkCapabilities(cm.getActiveNetwork());return c!=null&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}catch(Exception e){return false;}
    }
    void resume(){
        foreground=true;PowerManager pm=(PowerManager)activity.getSystemService(Context.POWER_SERVICE);interactive=pm==null||pm.isInteractive();loaded=false;reconcile();
    }
    void pause(){foreground=false;preview=false;generation++;handler.removeCallbacks(wake);weatherScheduled=false;view.setHostActive(false);}
    void screen(boolean on){interactive=on;if(!on)preview=false;reconcile();}
    void obscured(boolean value){obscured=value;reconcile();}
    private void settingsChanged(){
        RainOptions next=new RainOptions(prefs);
        if(!next.locationId.equals(options.locationId)){generation++;loaded=false;forecast=null;fromCache=false;loadedId="";problem="";}
        options=next;if(!options.enabled)preview=false;reconcile();
    }
    void preview(boolean value){preview=value&&options.enabled;reconcile();}
    boolean previewing(){return preview;}
    void refresh(){force=true;loaded=false;reconcile();}
    private boolean weatherWanted(){return foreground&&interactive&&(weather!=null||options.enabled&&options.forecastDriven&&!preview&&!options.locationId.isEmpty());}
    private void reconcile(){
        if(closed)return;
        handler.removeCallbacks(wake);weatherScheduled=false;view.configure(options);
        state=forecast!=null&&forecast.locationId.equals(options.locationId)?forecast.state(clock.instant()):WeatherForecast.State.UNKNOWN;
        activity.weatherWallpaper(state);
        boolean rainy=state==WeatherForecast.State.RAIN_EXPECTED||state==WeatherForecast.State.THUNDERSTORM_EXPECTED;
        view.setThunder(!preview&&options.forecastDriven&&state==WeatherForecast.State.THUNDERSTORM_EXPECTED);
        view.setRain(options.enabled&&(preview||options.forecastDriven&&rainy));
        view.setHostActive(foreground&&interactive&&!obscured&&options.enabled);
        if(weather!=null)weather.update(forecast,weatherId(),options.locationName,clock.instant(),fromCache);
        if(!weatherWanted())return;
        if(!loaded||!weatherId().equals(loadedId)){request();return;}
        long now=clock.millis();long deadline=Math.min(nextFetch,WeatherForecast.nextBoundary(clock.instant()).toEpochMilli());
        if(forecast!=null&&forecast.fresh(clock.instant()))deadline=Math.min(deadline,forecast.fetchedAt+WeatherForecast.FRESH_MILLIS);
        weatherScheduled=true;handler.postDelayed(wake,Math.max(60_000L,deadline-now));
    }
    private void request(){
        if(busy||closed)return;busy=true;
        final int token=generation;final String id=weatherId();final boolean manual=force;force=false;
        final WeatherForecast previous=forecast!=null&&id.equals(forecast.locationId)?forecast:null;
        final boolean previousFromCache=fromCache;
        final String previousProblem=problem;
        worker.execute(()->{
            WeatherForecast result=previous;boolean cached=previous!=null&&previousFromCache;
            String failure=previousProblem;long due=clock.millis()+REFRESH;
            try{
                if(result==null){result=source.cached(id);cached=result!=null;if(result==null&&!source.cacheProblem().isEmpty())failure=source.cacheProblem();}
                long now=clock.millis();long gate=Math.max(prefs.getLong("request_not_before",0),prefs.getLong("retry_at",0));
                long refreshAt=result==null||!result.fresh(clock.instant())||manual?now:result.fetchedAt+REFRESH;
                // An old projected cache may lack today's summary; retry missing display data within 15 minutes, not six hours.
                if(result!=null&&result.displayDescription(clock.instant()).isEmpty())refreshAt=Math.min(refreshAt,result.fetchedAt+FAILURE_RETRY);
                due=Math.max(gate,refreshAt);
                if(now>=due){
                    if(!network.getAsBoolean()){cached=result!=null;failure="Offline; no request sent";due=now+OFFLINE_RETRY;}
                    else{
                        // Cross-resume / cross-location cooldown: at most one request per minute.
                        prefs.edit().putLong("request_not_before",now+60_000L).apply();
                        try{result=source.fetch(id,now);cached=false;failure="";due=now+REFRESH;prefs.edit().putLong("retry_at",0).apply();}
                        catch(Exception e){long delay=e instanceof MetWeatherRepository.FetchFailure?((MetWeatherRepository.FetchFailure)e).retryMillis:FAILURE_RETRY;
                            cached=result!=null;due=now+delay;prefs.edit().putLong("retry_at",due).apply();failure=diagnose(e)+"; cache retained if fresh";}
                    }
                }
            }catch(Exception invalid){failure=diagnose(invalid);due=clock.millis()+FAILURE_RETRY;}
            final WeatherForecast answer=result;final boolean answerFromCache=cached;final String error=failure;final long when=due;
            handler.post(()->{
                busy=false;if(closed)return;
                if(token==generation&&id.equals(weatherId())){forecast=answer;fromCache=answerFromCache;loaded=true;loadedId=id;problem=error;nextFetch=when;}
                if(foreground)reconcile();
            });
        });
    }
    void showSettings(){new RainSettingsDialog(activity,this).show();}
    static String diagnose(Exception failure){
        if(failure instanceof MetWeatherRepository.FetchFailure)return "HTTP/API error: "+failure.getMessage();
        if(failure instanceof java.net.SocketTimeoutException)return "Network timeout: "+failure.getMessage();
        if(failure instanceof WeatherForecast.ParseFailure)return failure.getMessage();
        if(failure instanceof java.net.UnknownHostException)return "Network/DNS failure: "+failure.getMessage();
        if(failure instanceof javax.net.ssl.SSLException)return "Network/TLS failure: "+failure.getMessage();
        if(failure instanceof java.io.IOException)return "Network/I/O failure: "+failure.getMessage();
        return "Forecast error: "+failure.getClass().getSimpleName()+": "+failure.getMessage();
    }
    String status(){
        Instant now=clock.instant();String desc=forecast==null?"":forecast.displayDescription(now);
        String stamp=forecast==null?"None":Instant.ofEpochMilli(forecast.fetchedAt).atZone(WeatherForecast.ZONE).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm"));
        String url;try{url=MetWeatherRepository.endpoint(weatherId());}catch(Exception invalid){url="Invalid location";}
        return "Source: MET Malaysia via data.gov.my (CC BY 4.0). Daily forecast; NOT confirmed current rainfall.\n\n"+
                "Display location: "+options.locationName+"\nForecast location: "+weatherId()+" / "+(forecast==null?"Not retrieved":forecast.sourceName)+"\n"+
                (RainOptions.HOME_ID.equals(weatherId())?RainOptions.HOME_SCOPE+"\n":"")+
                "Request: "+url+"\nTV time (MYT): "+now.atZone(WeatherForecast.ZONE)+"\n"+
                "Availability: "+(forecast==null?"No usable cached/network forecast":forecast.availability(now))+"\n"+
                "Rain-effect state: "+state+"\nDescription: "+(desc.isEmpty()?"Unavailable":desc)+"\nRetrieved (MYT): "+stamp+
                "\nData origin: "+(forecast==null?"Unavailable":fromCache?"Cached / retained forecast":"Network retrieval this session")+
                "\nTransport: "+source.diagnostic()+"\nCache: "+source.cacheProblem()+"\nLast problem: "+(problem.isEmpty()?"None":problem)+
                "\nNext request not before (MYT): "+(nextFetch==0?"Pending":Instant.ofEpochMilli(nextFetch).atZone(WeatherForecast.ZONE))+
                (preview?"\nManual preview is active; independent of the weather state.":"")+
                "\n\nPeriod policy (MYT): morning 06:00–12:00, afternoon 12:00–19:00, night 19:00–06:00. Before 06:00 the previous date's night is requested. If that period is absent, Home may show today's summary explicitly labelled Ramalan harian. A daily summary NEVER triggers rain animation. These cutoffs are app policy, not MET observation timestamps.\n\nRefresh: six hours while Home is active; cache expires after 24 hours; offline retry 30 minutes; API/network retry 15 minutes or Retry-After. No current temperature, observation, rainfall measurement or location permission is used.";
    }
    WeatherForecast.State currentState(){return state;}
    boolean isBusy(){return busy;}
    boolean hasWeatherWake(){return weatherScheduled;}
    long nextFetchAt(){return nextFetch;}
    void destroy(){closed=true;foreground=false;generation++;prefs.unregisterOnSharedPreferenceChangeListener(changes);handler.removeCallbacksAndMessages(null);worker.shutdownNow();root.removeOnLayoutChangeListener(resize);root.getOverlay().remove(view);view.destroy();}
}
