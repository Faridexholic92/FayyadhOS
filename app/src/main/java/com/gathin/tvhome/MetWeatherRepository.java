package com.gathin.tvhome;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONObject;
import org.json.JSONArray;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

/** Dedicated bounded HTTPS/cache path; existing prayer transport is not altered. */
final class MetWeatherRepository implements RainController.Source {
    // The documented slashless path currently redirects (301). Use the verified canonical HTTPS URL.
    static final String ENDPOINT="https://api.data.gov.my/weather/forecast/";
    static final int MAX_BYTES=64*1024;
    private final AtomicFile cache;
    private volatile String cacheProblem="",lastResponse="";
    @Override public String cacheProblem(){return cacheProblem;}
    @Override public String diagnostic(){return lastResponse;}
    MetWeatherRepository(Context c){cache=new AtomicFile(new File(c.getFilesDir(),"met-rain-forecast.json"));}
    static String endpoint(String id)throws Exception{
        if(!WeatherForecast.validId(id))throw new IllegalArgumentException("Invalid MET location");
        return ENDPOINT+"?filter="+URLEncoder.encode(id+"@location__location_id","UTF-8")+
                "&sort=date&limit=14";
    }
    @Override public WeatherForecast cached(String id){
        try{JSONObject saved=new JSONObject(read(cache.openRead()));
            if(!id.equals(saved.getString("location_id"))){cacheProblem="Cached location differs from selected "+id;return null;}
            cacheProblem="";
            return WeatherForecast.parse(saved.getJSONArray("rows").toString(),id,saved.getLong("fetched_at"));
        }catch(FileNotFoundException missing){cacheProblem="No cached data";return null;}
        catch(Exception unavailable){cacheProblem="Invalid cached data: "+unavailable.getMessage();return null;}
    }
    @Override public WeatherForecast fetch(String id,long now)throws Exception{
        lastResponse="Request started: "+endpoint(id);
        HttpsURLConnection connection=(HttpsURLConnection)new URL(endpoint(id)).openConnection();
        connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/json");String raw;
        try{
            int status=connection.getResponseCode();String type=connection.getContentType();
            lastResponse="HTTP "+status+"; Content-Type: "+type;
            if(status==200&&(type==null||!type.toLowerCase(java.util.Locale.ROOT).contains("application/json")))
                throw new WeatherForecast.ParseFailure("Unexpected response content type: "+type);
            if(status!=200)throw new FetchFailure(status,status==429?retryDelay(connection.getHeaderField("Retry-After"),now):15*60*1000L);
            raw=read(connection.getInputStream());
        }finally{connection.disconnect();}
        WeatherForecast forecast=WeatherForecast.parse(raw,id,now); // validate before replacing a usable cache
        lastResponse+="; parsed "+forecast.days.size()+" records for "+forecast.locationId+" / "+forecast.sourceName;
        JSONObject saved=new JSONObject().put("location_id",id).put("fetched_at",now).put("rows",new JSONArray(raw));
        FileOutputStream out=null;
        try{out=cache.startWrite();out.write(saved.toString().getBytes(StandardCharsets.UTF_8));cache.finishWrite(out);cacheProblem="";}
        catch(IOException failure){if(out!=null)cache.failWrite(out);cacheProblem="Cache write failed; network data usable this session: "+failure.getMessage();}
        return forecast;
    }
    static long retryDelay(String header,long now){
        long delay=15*60*1000L;
        try{delay=Math.multiplyExact(Long.parseLong(header),1000L);}catch(Exception notSeconds){
            try{delay=ZonedDateTime.parse(header,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-now;}catch(Exception ignored){}
        }
        return Math.max(60*1000L,Math.min(24*60*60*1000L,delay));
    }
    static String read(InputStream stream)throws IOException{
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("MET response too large");out.write(buffer,0,n);}
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    static final class FetchFailure extends IOException {
        final long retryMillis;
        FetchFailure(int code,long delay){super("MET HTTP "+code);retryMillis=delay;}
    }
}
