package com.gathin.tvhome;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.*;

/** One foreground-only, bounded location request. No IP lookup or background tracking. */
final class DeviceLocation {
    interface Result {void found(Location location);void failed(String reason);}
    interface Source {void request(Result result);void cancel();}
    static final long MAX_AGE_MS=2*60*60*1000L,TIMEOUT_MS=25_000;
    static boolean permitted(Context c){return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static boolean usable(Location location,long now,long elapsedNanos){
        if(location==null||!HourlyWeatherRepository.validCoordinates(location.getLatitude(),location.getLongitude())||!location.hasAccuracy()||!Float.isFinite(location.getAccuracy())||location.getAccuracy()<0||location.getAccuracy()>50_000)return false;
        long age=location.getElapsedRealtimeNanos()>0?(elapsedNanos-location.getElapsedRealtimeNanos())/1_000_000:now-location.getTime();
        return age>=0&&age<=MAX_AGE_MS;
    }
    static final class AndroidSource implements Source,LocationListener {
        private final Context context;private final LocationManager manager;private final Handler main=new Handler(Looper.getMainLooper());private Result result;
        private final Runnable timeout=()->fail("Lokasi belum diperoleh. Pastikan Location TV ON; cuba semula atau guna lokasi manual.");
        AndroidSource(Context context){this.context=context;manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);}
        @Override public void request(Result callback){
            cancel();result=callback;
            if(!permitted(context)){fail("Akses lokasi belum dibenarkan.");return;}
            if(manager==null){fail("TV ini tiada perkhidmatan lokasi Android.");return;}
            try{
                if(Build.VERSION.SDK_INT>=28&&!manager.isLocationEnabled()){fail("Location TV OFF. Hidupkan dalam tetapan Android.");return;}
                List<String> enabled=manager.getProviders(true),providers=new ArrayList<>();
                for(String name:Arrays.asList(LocationManager.NETWORK_PROVIDER,"fused",LocationManager.GPS_PROVIDER))if(enabled.contains(name)&&(!name.equals(LocationManager.GPS_PROVIDER)||context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED))providers.add(name);
                Location newest=null;
                for(String name:enabled)try{Location value=manager.getLastKnownLocation(name);if(usable(value,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos())&&(newest==null||value.getTime()>newest.getTime()))newest=value;}catch(SecurityException ignored){}
                if(newest!=null){found(newest);return;}
                if(providers.isEmpty()){fail("TV ini tidak menyediakan network/GPS location. Permission sahaja tidak menghasilkan lokasi. Guna lokasi manual atau semak Location TV.");return;}
                int requested=0;
                for(String name:providers)try{manager.requestLocationUpdates(name,0,0,this,Looper.getMainLooper());requested++;}catch(IllegalArgumentException|SecurityException ignored){}
                if(requested==0){fail("Penyedia lokasi TV tidak dapat digunakan. Semak permission dan Location TV.");return;}
                main.postDelayed(timeout,TIMEOUT_MS);
            }catch(RuntimeException unavailable){fail("Perkhidmatan lokasi TV tidak tersedia. Semak tetapan Android.");}
        }
        @Override public void onLocationChanged(Location location){if(usable(location,System.currentTimeMillis(),SystemClock.elapsedRealtimeNanos()))found(location);}
        @Override public void onProviderEnabled(String name){}
        @Override public void onProviderDisabled(String name){}
        @Override public void onStatusChanged(String provider,int status,Bundle extras){}
        private void found(Location value){Result callback=result;cancel();if(callback!=null)callback.found(new Location(value));}
        private void fail(String reason){Result callback=result;cancel();if(callback!=null)callback.failed(reason);}
        @Override public void cancel(){main.removeCallbacks(timeout);result=null;if(manager!=null)try{manager.removeUpdates(this);}catch(RuntimeException ignored){}}
    }
}
