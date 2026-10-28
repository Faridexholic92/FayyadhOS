package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.TextView;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Keyless ZenQuotes batch, once/day cache, short quotes only; no location or identifier sent. */
final class DailyQuotes {
    static final String PREFS="daily-quotes",SOURCE="https://zenquotes.io/";
    interface Source {String fetch()throws Exception;}
    static final class Quote {final String text,author;Quote(String t,String a){text=t;author=a;}}
    private final DashboardActivity activity;private final TextView text,credit;private final SharedPreferences prefs;
    private final Source source;private final Clock clock;private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean active,closed,busy;
    private final Runnable tick=this::reconcile;
    // Original fallback microcopy, not attributed to a historical person or to ZenQuotes.
    private static final String[] FALLBACK={"Make room for small joys.","Take one good step today.","A little kindness goes a long way.","Pause. Breathe. Begin again.","Do less, with more care.","Keep a little time for yourself.","Notice something good today."};
    static boolean enabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean("enabled",true);}
    DailyQuotes(DashboardActivity a,TextView text,TextView credit){this(a,text,credit,DailyQuotes::fetch,Clock.systemUTC());}
    DailyQuotes(DashboardActivity a,TextView text,TextView credit,Source source,Clock clock){activity=a;this.text=text;this.credit=credit;this.source=source;this.clock=clock;prefs=a.getSharedPreferences(PREFS,0);render();credit.setFocusable(true);credit.setOnClickListener(v->details());GlassUi.focusShortcut(credit);}
    private static String fetch()throws Exception{
        HttpsURLConnection c=(HttpsURLConnection)new URL("https://zenquotes.io/api/quotes").openConnection();
        c.setConnectTimeout(6000);c.setReadTimeout(6000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        try{if(c.getResponseCode()!=200)throw new java.io.IOException("Quote service unavailable");String type=c.getContentType();if(type==null||!type.toLowerCase(Locale.ROOT).contains("application/json"))throw new java.io.IOException("Unexpected quote response");return HourlyWeatherRepository.read(c.getInputStream(),SystemClock.elapsedRealtime()+15_000);}finally{c.disconnect();}
    }
    static List<Quote> parse(String raw)throws JSONException{
        if(raw==null||raw.length()>256*1024)throw new JSONException("Quote response too large");
        JSONArray array=new JSONArray(raw);if(array.length()>100)throw new JSONException("Too many quotes");List<Quote> quotes=new ArrayList<>();
        for(int i=0;i<array.length();i++){
            JSONObject row=array.optJSONObject(i);if(row==null||!(row.opt("q") instanceof String)||!(row.opt("a") instanceof String))continue;
            String text=row.getString("q").replaceAll("\\s+"," ").trim(),author=row.getString("a").trim();
            if(text.length()<8||text.length()>110||author.isEmpty()||author.length()>50||text.contains("<")||text.contains(">"))continue;
            quotes.add(new Quote(text,author));
        }
        return quotes;
    }
    static Quote choose(List<Quote> values,LocalDate date){return values.isEmpty()?new Quote(FALLBACK[Math.floorMod(date.toEpochDay(),FALLBACK.length)],""):values.get(Math.floorMod(date.toEpochDay(),values.size()));}
    private LocalDate day(){return clock.instant().atZone(WeatherForecast.ZONE).toLocalDate();}
    private void render(){
        List<Quote> values=Collections.emptyList();try{values=parse(prefs.getString("batch","[]"));}catch(Exception ignored){}
        Quote quote=choose(values,day());boolean external=!values.isEmpty();text.setText("“"+quote.text+"”");
        credit.setText(external?"— "+quote.author+" · ZenQuotes":"Daily life · FayyadhOS");
        credit.setContentDescription(external?"Inspirational quotes provided by ZenQuotes API. https://zenquotes.io/. Press OK for source and controls.":"Original FayyadhOS daily reflection. Press OK for quote controls.");
    }
    void setEnabled(boolean enabled){prefs.edit().putBoolean("enabled",enabled).apply();reconcile();}
    void resume(){active=true;reconcile();}
    void pause(){active=false;main.removeCallbacks(tick);}
    private void reconcile(){
        if(closed)return;main.removeCallbacks(tick);render();if(!active||!enabled(activity))return;
        main.postDelayed(tick,60_000);
        if(busy||day().toString().equals(prefs.getString("fetched_day",""))||clock.millis()<prefs.getLong("retry_at",0))return;
        busy=true;prefs.edit().putLong("retry_at",clock.millis()+6*60*60*1000L).apply();
        worker.execute(()->{
            try{String raw=source.fetch();if(parse(raw).isEmpty())throw new java.io.IOException("No short quotes");
                if(!closed)prefs.edit().putString("batch",raw).putString("fetched_day",day().toString()).putLong("retry_at",0).apply();
            }catch(Exception ignored){/* Keep the previous short quote pool or original offline reflection. */}
            main.post(()->{busy=false;if(!closed&&active)render();});
        });
    }
    void details(){
        activity.show(new AlertDialog.Builder(activity).setTitle("Daily life quotes")
            .setMessage("Inspirational quotes provided by ZenQuotes API — https://zenquotes.io/\n\nFree keyless service; one cached batch per day, short entries only. The quote changes by your Malaysian calendar day. No location or user identifier is sent. If unavailable, cached quotes or original FayyadhOS reflections remain.\n\nOnline quotes: "+(enabled(activity)?"ON":"OFF"))
            .setPositiveButton("Visit ZenQuotes",(d,i)->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(SOURCE)));}catch(Exception e){activity.message("ZenQuotes",SOURCE);}})
            .setNeutralButton(enabled(activity)?"Online: OFF":"Online: ON",(d,i)->setEnabled(!enabled(activity)))
            .setNegativeButton("Close",null));
    }
    void destroy(){closed=true;active=false;main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
