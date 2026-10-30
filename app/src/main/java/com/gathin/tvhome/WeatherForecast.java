package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;
import java.time.*;
import java.util.*;

/** MET forecast, never an instantaneous rainfall observation. No summary/warning inference. */
final class WeatherForecast {
    enum State { CLEAR, CLOUDY, RAIN_EXPECTED, THUNDERSTORM_EXPECTED, UNKNOWN }
    static final ZoneId ZONE=ZoneId.of("Asia/Kuala_Lumpur");
    static final long FRESH_MILLIS=24*60*60*1000L;
    final String locationId;
    final long fetchedAt;
    final Map<LocalDate,String[]> days;
    private final Map<LocalDate,String> temperatures,summaries;
    final String sourceName;
    static final class ParseFailure extends IllegalArgumentException {
        ParseFailure(String message){super(message);}
    }
    WeatherForecast(String id,long stamp,Map<LocalDate,String[]> days){this(id,stamp,days,Collections.emptyMap(),Collections.emptyMap(),id);}
    private WeatherForecast(String id,long stamp,Map<LocalDate,String[]> days,Map<LocalDate,String> temperatures,Map<LocalDate,String> summaries,String sourceName){
        this.summaries=Collections.unmodifiableMap(new TreeMap<>(summaries));this.sourceName=sourceName;
        this.temperatures=Collections.unmodifiableMap(new TreeMap<>(temperatures));
        locationId=id;fetchedAt=stamp;this.days=Collections.unmodifiableMap(new TreeMap<>(days));
    }
    static boolean validId(String id){return id!=null&&id.matches("(?:St|Rc|Ds|Tn|Dv)[0-9]{3}");}
    static WeatherForecast parse(String raw,String id,long fetchedAt)throws Exception{
        if(!validId(id)||fetchedAt<=0)throw new IllegalArgumentException("Invalid forecast identity/time");
        Object root;
        try{
            if(raw==null||(!raw.trim().startsWith("[")&&!raw.trim().startsWith("{")))throw new JSONException("Not JSON");
            JSONTokener tokens=new JSONTokener(raw);root=tokens.nextValue();
            if(tokens.nextClean()!=0)throw new JSONException("Trailing content");
        }catch(JSONException bad){throw new ParseFailure("Invalid JSON in MET response");}
        if(!(root instanceof JSONArray))throw new ParseFailure("Unexpected response structure: expected JSON array");
        JSONArray rows=(JSONArray)root;
        if(rows.length()==0)throw new ParseFailure("No records returned by MET API");
        if(rows.length()>32)throw new ParseFailure("Unexpected response structure: too many records");
        Map<LocalDate,String[]> result=new TreeMap<>();
        Map<LocalDate,String> temperatures=new TreeMap<>(),summaries=new TreeMap<>();
        String sourceName=id;int matching=0,invalidRows=0;
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);JSONObject location=row==null?null:row.optJSONObject("location");
            if(location==null){invalidRows++;continue;}
            if(!id.equals(location.optString("location_id","")))continue;
            matching++;String name=optionalText(location,"location_name");if(!name.isEmpty())sourceName=name;
            LocalDate date;
            try{date=LocalDate.parse(row.optString("date",""));}catch(Exception badDate){invalidRows++;continue;}
            // Periods, summaries and temperatures may be individually absent/null. Keep other valid data.
            String[] text={optionalText(row,"morning_forecast"),optionalText(row,"afternoon_forecast"),optionalText(row,"night_forecast")};
            String summary=optionalText(row,"summary_forecast");if(!summary.isEmpty())summaries.put(date,summary);
            Object low=row.opt("min_temp"),high=row.opt("max_temp");
            if(low instanceof Number&&high instanceof Number){
                double min=((Number)low).doubleValue(),max=((Number)high).doubleValue();
                if(Double.isFinite(min)&&Double.isFinite(max)&&min>=-90&&max<=60&&min<=max&&min==Math.rint(min)&&max==Math.rint(max))
                    temperatures.put(date,(int)min+"–"+(int)max+"°C");
            }
            if(result.put(date,text)!=null)throw new ParseFailure("Unexpected response structure: duplicate forecast date");
        }
        if(matching==0){if(invalidRows>0)throw new ParseFailure("Unexpected response structure: missing location object");
            throw new ParseFailure("No records matching selected location "+id);}
        if(result.isEmpty())throw new ParseFailure("Unexpected response structure: no valid forecast dates");
        return new WeatherForecast(id,fetchedAt,result,temperatures,summaries,sourceName);
    }
    private static String optionalText(JSONObject row,String key){
        Object value=row.opt(key);if(!(value instanceof String))return "";
        String text=((String)value).trim();return text.length()<=240?text:"";
    }
    static State classify(String value){
        if(value==null)return State.UNKNOWN;
        String s=value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
        if(s.equals("tiada hujan"))return State.CLEAR;
        if(s.equals("berawan")||s.equals("mendung")||s.equals("berjerebu"))return State.CLOUDY;
        // Known documented variants plus "kebanyakan"/"menyeluruh" verified in live API.
        String suffix="(?: menyeluruh| di (?:satu dua|beberapa|kebanyakan) tempat(?: di kawasan (?:pantai|pedalaman))?)?";
        if(s.matches("ribut petir"+suffix))return State.THUNDERSTORM_EXPECTED;
        if(s.matches("hujan"+suffix))return State.RAIN_EXPECTED;
        return State.UNKNOWN; // Unrecognised/conditional descriptions fail closed.
    }
    boolean fresh(Instant now){long age=now.toEpochMilli()-fetchedAt;return age>=0&&age<FRESH_MILLIS;}
    static int period(ZonedDateTime now){int h=now.getHour();return h>=6&&h<12?0:h>=12&&h<19?1:2;}
    static LocalDate forecastDate(ZonedDateTime now){return now.getHour()<6?now.toLocalDate().minusDays(1):now.toLocalDate();}
    String description(Instant instant){ZonedDateTime now=instant.atZone(ZONE);String[] day=days.get(forecastDate(now));return day==null?"":day[period(now)];}
    String temperatureRange(Instant now){return fresh(now)?temperatures.getOrDefault(now.atZone(ZONE).toLocalDate(),""):"";}
    // Display-only daily fallback. Never use a daily summary to trigger the rain animation.
    boolean usesDailySummary(Instant now){return description(now).isEmpty()&&!summaries.getOrDefault(now.atZone(ZONE).toLocalDate(),"").isEmpty();}
    String displayDescription(Instant now){return usesDailySummary(now)?summaries.get(now.atZone(ZONE).toLocalDate()):description(now);}
    String displayLabel(Instant now){return usesDailySummary(now)?"Ramalan harian":"Ramalan";}
    State displayState(Instant now){return fresh(now)?classify(displayDescription(now)):State.UNKNOWN;}
    String availability(Instant now){
        long age=now.toEpochMilli()-fetchedAt;
        if(age<0)return "Cache timestamp is in the future; check TV date/time";
        if(age>=FRESH_MILLIS)return "Expired cached data (24-hour maximum)";
        String period=forecastDate(now.atZone(ZONE))+" / "+new String[]{"morning","afternoon","night"}[period(now.atZone(ZONE))];
        if(description(now).isEmpty())return "Missing forecast for requested MYT date/period "+period+
                (usesDailySummary(now)?"; showing today's DAILY summary, not current-period weather":"; no daily summary available");
        return "Available: "+period;
    }
    static final String[] PERIOD_NAMES={"Pagi","Petang","Malam"};
    /** MET forecast for the period {@code steps} after the current MYT period; empty when absent/stale. */
    String upcoming(Instant instant,int steps){
        if(!fresh(instant))return "";
        ZonedDateTime now=instant.atZone(ZONE);LocalDate date=forecastDate(now);int p=period(now);
        for(int i=0;i<steps;i++){p++;if(p>2){p=0;date=date.plusDays(1);}}
        String[] day=days.get(date);if(day==null||day[p].isEmpty())return "";
        String label=date.isAfter(now.toLocalDate())?"Esok "+PERIOD_NAMES[p].toLowerCase(Locale.ROOT):PERIOD_NAMES[p];
        return label+": "+day[p];
    }
    State state(Instant now){return fresh(now)?classify(description(now)):State.UNKNOWN;}
    static Instant nextBoundary(Instant instant){
        ZonedDateTime now=instant.atZone(ZONE);int h=now.getHour();
        ZonedDateTime next=h<6?now.withHour(6):h<12?now.withHour(12):h<19?now.withHour(19):now.plusDays(1).withHour(6);
        return next.withMinute(0).withSecond(0).withNano(0).toInstant();
    }
}
