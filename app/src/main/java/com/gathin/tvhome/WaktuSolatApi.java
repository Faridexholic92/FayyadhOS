package com.gathin.tvhome;
import org.json.*;
import java.time.*;
import java.util.*;
/** API v2 uses epoch SECONDS. Validate every date in Malaysia, zone, month and time order. */
final class WaktuSolatApi {
 static Map<LocalDate,PrayerTimes.Day> parse(String raw,String zone,YearMonth month) throws Exception {
  JSONObject root=new JSONObject(raw);
  if(!zone.equals(root.getString("zone")) || root.getInt("year")!=month.getYear() || root.getInt("month_number")!=month.getMonthValue()) throw new IllegalArgumentException("API zone/month mismatch");
  JSONArray rows=root.getJSONArray("prayers"); Map<LocalDate,PrayerTimes.Day> days=new TreeMap<>();
  for(int n=0;n<rows.length();n++) {
   JSONObject row=rows.getJSONObject(n); LocalDate date=month.atDay(row.getInt("day"));
   LocalTime[] times=new LocalTime[6];
   for(int i=0;i<6;i++) {
    Object number=row.get(PrayerTimes.KEYS[i]);if(!(number instanceof Number)) throw new IllegalArgumentException("Invalid timestamp type");
    ZonedDateTime at=Instant.ofEpochSecond(((Number)number).longValue()).atZone(PrayerTimes.MALAYSIA);
    if(!at.toLocalDate().equals(date)) throw new IllegalArgumentException("Wrong epoch date");
    times[i]=at.toLocalTime();if(i>0 && !times[i].isAfter(times[i-1])) throw new IllegalArgumentException("Unordered times");
   }
   if(days.put(date,new PrayerTimes.Day(date,row.optString("hijri"),times))!=null) throw new IllegalArgumentException("Duplicate day");
  }
  if(days.isEmpty()) throw new IllegalArgumentException("Empty schedule");return days;
 }
}
