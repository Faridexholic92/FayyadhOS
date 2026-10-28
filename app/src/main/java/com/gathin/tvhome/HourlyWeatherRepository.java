package com.gathin.tvhome;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Separate opt-in provider; MET remains the sole weather-wallpaper/rain driver. */
final class HourlyWeatherRepository implements HourlyWeatherController.Source {
    private final AtomicFile cache;
    HourlyWeatherRepository(Context context){cache=new AtomicFile(new File(context.getFilesDir(),"open-meteo-current-v2.json"));}
    static boolean validCoordinates(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180;}
    static String location(double lat,double lon){if(!validCoordinates(lat,lon))throw new IllegalArgumentException("Invalid coordinates");return String.format(Locale.ROOT,"%.5f,%.5f",lat,lon);}
    static String endpoint(double lat,double lon){
        if(!validCoordinates(lat,lon))throw new IllegalArgumentException("Invalid coordinates");
        return String.format(Locale.ROOT,"https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,weather_code&temperature_unit=celsius&timeformat=unixtime&timezone=Asia%%2FKuala_Lumpur&forecast_days=1",lat,lon);
    }
    @Override public HourlyWeather cached(double lat,double lon){
        try{JSONObject saved=new JSONObject(read(cache.openRead(),0));if(!location(lat,lon).equals(saved.getString("location")))return null;return HourlyWeather.parseCurrent(saved.getString("raw"),saved.getLong("fetched_at"));}catch(Exception invalid){return null;}
    }
    @Override public HourlyWeather fetch(double lat,double lon,long now)throws Exception{
        HttpsURLConnection connection=(HttpsURLConnection)new URL(endpoint(lat,lon)).openConnection();
        connection.setConnectTimeout(8000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("Accept","application/json");
        String raw;long deadline=android.os.SystemClock.elapsedRealtime()+20_000;
        try{
            int status=connection.getResponseCode();if(status!=200)throw new Failure("Open-Meteo HTTP "+status,status==429?MetWeatherRepository.retryDelay(connection.getHeaderField("Retry-After"),now):900_000);
            String type=connection.getContentType();if(type==null||!type.toLowerCase(Locale.ROOT).contains("application/json"))throw new IOException("Unexpected response type");
            raw=read(connection.getInputStream(),deadline);
        }finally{connection.disconnect();}
        HourlyWeather result=HourlyWeather.parseCurrent(raw,now);
        JSONObject saved=new JSONObject().put("location",location(lat,lon)).put("fetched_at",now).put("raw",raw);
        FileOutputStream out=null;
        try{out=cache.startWrite();out.write(saved.toString().getBytes(StandardCharsets.UTF_8));cache.finishWrite(out);}catch(IOException e){if(out!=null)cache.failWrite(out);/* validated network result remains usable */}
        return result;
    }
    static String read(InputStream stream,long deadline)throws IOException{
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];int count;
            while((count=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted()||deadline!=0&&android.os.SystemClock.elapsedRealtime()>deadline)throw new IOException("Weather request cancelled/timed out");if(out.size()+count>256*1024)throw new IOException("Weather response too large");out.write(buffer,0,count);}return out.toString("UTF-8");
        }
    }
    static final class Failure extends IOException {final long retry;Failure(String message,long retry){super(message);this.retry=retry;}}
}
