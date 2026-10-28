package com.gathin.tvhome;
import java.time.*;import java.util.*;import org.json.*;import org.junit.Test;import static org.junit.Assert.*;
public class WaktuSolatApiTest {
 String raw(String zone)throws Exception{try(var in=getClass().getResourceAsStream("/waktusolat-"+zone+"-2026-10.json")){return new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
 Map<LocalDate,PrayerTimes.Day> parse(String json)throws Exception{return WaktuSolatApi.parse(json,"SWK08",YearMonth.of(2026,10));}
 @Test public void liveSwkFixtureParsesAll31DaysAndEpochSeconds()throws Exception{var days=parse(raw("SWK08"));assertEquals(31,days.size());var day=days.get(LocalDate.of(2026,10,24));assertEquals(LocalTime.of(5,14),day.times[0]);assertEquals(LocalTime.of(19,38),day.times[5]);}
 @Test public void liveWlyFixtureIsDistinctAndValid()throws Exception{var days=WaktuSolatApi.parse(raw("WLY01"),"WLY01",YearMonth.of(2026,10));assertEquals(LocalTime.of(5,49),days.get(LocalDate.of(2026,10,24)).times[0]);}
 @Test public void wrongZoneRejected()throws Exception{assertThrows(IllegalArgumentException.class,()->parse(raw("WLY01")));}
 @Test public void wrongMonthRejected()throws Exception{var j=new JSONObject(raw("SWK08")).put("month_number",11);assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void wrongYearRejected()throws Exception{var j=new JSONObject(raw("SWK08")).put("year",2027);assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void duplicateDateRejected()throws Exception{var j=new JSONObject(raw("SWK08"));var a=j.getJSONArray("prayers");a.put(a.getJSONObject(0));assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void millisecondEpochRejected()throws Exception{var j=new JSONObject(raw("SWK08"));var r=j.getJSONArray("prayers").getJSONObject(0);r.put("fajr",r.getLong("fajr")*1000);assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void unorderedTimesRejected()throws Exception{var j=new JSONObject(raw("SWK08"));var r=j.getJSONArray("prayers").getJSONObject(0);r.put("asr",r.getLong("fajr"));assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void emptyScheduleRejected()throws Exception{var j=new JSONObject(raw("SWK08")).put("prayers",new JSONArray());assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void missingTimeRejected()throws Exception{var j=new JSONObject(raw("SWK08"));j.getJSONArray("prayers").getJSONObject(0).remove("isha");assertThrows(Exception.class,()->parse(j.toString()));}
 @Test public void foreignDateEpochRejected()throws Exception{var j=new JSONObject(raw("SWK08"));var r=j.getJSONArray("prayers").getJSONObject(0);r.put("isha",r.getLong("isha")+86400);assertThrows(IllegalArgumentException.class,()->parse(j.toString()));}
 @Test public void phoneTimezoneDoesNotChangeApiTimes()throws Exception{TimeZone before=TimeZone.getDefault();try{TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));assertEquals(LocalTime.of(5,14),parse(raw("SWK08")).get(LocalDate.of(2026,10,24)).times[0]);}finally{TimeZone.setDefault(before);}}
}
