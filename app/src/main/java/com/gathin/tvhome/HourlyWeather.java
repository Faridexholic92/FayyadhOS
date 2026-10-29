package com.gathin.tvhome;

import org.json.*;
import java.time.*;
import java.util.*;

/** Open-Meteo modelled current conditions and hourly forecast, not an on-site observation. */
final class HourlyWeather {
    static final long FRESH_MS=2*60*60*1000L;
    static final class Point {
        final long time;final double temperature;final int code;
        Point(long time,double temperature,int code){this.time=time;this.temperature=temperature;this.code=code;}
    }
    final long fetchedAt;final Point current;final List<Point> hours;
    HourlyWeather(long fetchedAt,Point current,List<Point> hours){this.fetchedAt=fetchedAt;this.current=current;this.hours=Collections.unmodifiableList(hours);}
    static double number(JSONObject object,String key)throws JSONException{
        Object raw=object.get(key);if(!(raw instanceof Number))throw new JSONException("Expected number: "+key);
        double value=((Number)raw).doubleValue();if(!Double.isFinite(value))throw new JSONException("Non-finite "+key);return value;
    }
    static Point point(double time,double temperature,double code)throws JSONException{
        if(time<=0||time!=Math.rint(time)||temperature< -90||temperature>65||code!=Math.rint(code)||!validCode((int)code))throw new JSONException("Invalid weather point");
        return new Point((long)time,temperature,(int)code);
    }
    static boolean validCode(int code){return code==0||code==1||code==2||code==3||code==45||code==48||code==56||code==66||code>=51&&code<=57&&code%2==1||code>=61&&code<=67&&code%2==1||code>=71&&code<=77&&code%2==1||code>=80&&code<=82||code==85||code==86||code==95||code==96||code==99;}
    static HourlyWeather parse(String raw,long fetchedAt)throws JSONException{
        JSONObject json=new JSONObject(raw),current=json.getJSONObject("current");
        JSONObject cu=json.getJSONObject("current_units"),hu=json.getJSONObject("hourly_units");
        if(!"unixtime".equals(cu.getString("time"))||!"°C".equals(cu.getString("temperature_2m"))||!"unixtime".equals(hu.getString("time"))||!"°C".equals(hu.getString("temperature_2m")))throw new JSONException("Unexpected units");
        Point now=point(number(current,"time"),number(current,"temperature_2m"),number(current,"weather_code"));
        if(Math.abs(now.time*1000L-fetchedAt)>2*60*60*1000L)throw new JSONException("Current timestamp outside freshness window");
        JSONObject hourly=json.getJSONObject("hourly");JSONArray times=hourly.getJSONArray("time"),temperatures=hourly.getJSONArray("temperature_2m"),codes=hourly.getJSONArray("weather_code");
        if(times.length()<7||times.length()>96||times.length()!=temperatures.length()||times.length()!=codes.length())throw new JSONException("Incomplete hourly arrays");
        List<Point> hours=new ArrayList<>();long previous=0;
        for(int i=0;i<times.length();i++){
            Object t=times.get(i),v=temperatures.get(i),c=codes.get(i);
            if(!(t instanceof Number)||!(v instanceof Number)||!(c instanceof Number))throw new JSONException("Null/non-numeric hourly data");
            Point point=point(((Number)t).doubleValue(),((Number)v).doubleValue(),((Number)c).doubleValue());
            if(previous!=0&&point.time-previous!=3600)throw new JSONException("Hourly data not contiguous");previous=point.time;hours.add(point);
        }
        HourlyWeather result=new HourlyWeather(fetchedAt,now,hours);
        if(result.after(Instant.ofEpochMilli(fetchedAt),3)==null||result.after(Instant.ofEpochMilli(fetchedAt),6)==null)throw new JSONException("Missing +3h/+6h coverage");
        return result;
    }
    static HourlyWeather parseCurrent(String raw,long fetchedAt)throws JSONException{
        JSONObject json=new JSONObject(raw),units=json.getJSONObject("current_units"),value=json.getJSONObject("current");
        if(!"unixtime".equals(units.getString("time"))||!"°C".equals(units.getString("temperature_2m")))throw new JSONException("Unexpected current units");
        Point point=point(number(value,"time"),number(value,"temperature_2m"),number(value,"weather_code"));
        if(Math.abs(point.time*1000L-fetchedAt)>2*60*60*1000L)throw new JSONException("Current timestamp outside freshness window");
        return new HourlyWeather(fetchedAt,point,Collections.emptyList());
    }
    boolean fresh(Instant now){long age=now.toEpochMilli()-fetchedAt;return age>=0&&age<FRESH_MS;}
    Point current(Instant now){return fresh(now)&&Math.abs(now.getEpochSecond()-current.time)<2*3600?current:null;}
    Point after(Instant now,int offset){long target=Math.floorDiv(now.getEpochSecond(),3600)*3600+offset*3600;for(Point p:hours)if(p.time==target)return p;return null;}
    static String description(int code){
        if(code==0)return "Cerah";if(code<=2)return "Sebahagian berawan";if(code==3)return "Berawan";
        if(code==45||code==48)return "Berkabus";if(code>=95)return "Ribut petir";
        if(code>=80&&code<=82)return code==82?"Hujan lebat":"Hujan berselang";
        if(code>=71&&code<=77||code==85||code==86)return "Salji";
        if(code>=61&&code<=67)return code==65||code==67?"Hujan lebat":"Hujan";
        return "Hujan renyai";
    }
    static int icon(int code){
        if(code==0)return R.drawable.ic_weather_sunny;
        if(code<=3)return R.drawable.ic_weather_cloudy;
        if(code>=95)return R.drawable.ic_weather_thunder;
        if(code>=51&&code<=67||code>=80&&code<=82)return R.drawable.ic_weather_rain;
        return R.drawable.ic_weather_unknown;
    }
}
