package com.gathin.tvhome;
import org.junit.Test;
import org.json.*;
import java.io.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
public class WeatherForecastTest {
 static final String ID="Ds554";
 static final Instant NOW=Instant.parse("2026-10-24T05:00:00Z");
 static String raw(String morning,String afternoon,String night)throws Exception{
  JSONArray rows=new JSONArray();
  for(int d=-1;d<=1;d++)rows.put(new JSONObject().put("location",new JSONObject().put("location_id",ID).put("location_name","Kota Kinabalu"))
   .put("date",NOW.atZone(WeatherForecast.ZONE).toLocalDate().plusDays(d).toString()).put("morning_forecast",morning).put("afternoon_forecast",afternoon).put("night_forecast",night));
  return rows.toString();
 }
 static WeatherForecast data(String text,long age)throws Exception{return WeatherForecast.parse(raw(text,text,text),ID,NOW.toEpochMilli()-age);}
 @Test public void noRainIsNotClassifiedAsRain(){assertEquals(WeatherForecast.State.CLEAR,WeatherForecast.classify(" Tiada  Hujan "));}
 @Test public void documentedRainAndCoastalVariants(){for(String s:new String[]{"Hujan","Hujan di beberapa tempat","Hujan di satu dua tempat di kawasan pantai","Hujan menyeluruh"})assertEquals(s,WeatherForecast.State.RAIN_EXPECTED,WeatherForecast.classify(s));}
 @Test public void stormVariantsVerifiedInActualApi(){for(String s:new String[]{"Ribut petir","Ribut petir di kebanyakan tempat","Ribut petir menyeluruh","Ribut petir di satu dua tempat di kawasan pedalaman"})assertEquals(WeatherForecast.State.THUNDERSTORM_EXPECTED,WeatherForecast.classify(s));}
 @Test public void cloudyHazyAndUnknownNeverTriggerRain(){for(String s:new String[]{"Berawan","Mendung","Berjerebu"})assertEquals(WeatherForecast.State.CLOUDY,WeatherForecast.classify(s));for(String s:new String[]{"rain maybe","Tiada hujan tetapi ribut", "Hujan tidak dijangka",""})assertEquals(WeatherForecast.State.UNKNOWN,WeatherForecast.classify(s));assertEquals(WeatherForecast.State.UNKNOWN,WeatherForecast.classify(null));}
 @Test public void currentPeriodOnlyNotDailySummary()throws Exception{WeatherForecast f=WeatherForecast.parse(raw("Hujan","Tiada Hujan","Ribut petir"),ID,NOW.toEpochMilli());assertEquals(WeatherForecast.State.CLEAR,f.state(NOW));}
 @Test public void freshnessHasStrictExpiryAndRejectsFutureClock()throws Exception{assertEquals(WeatherForecast.State.RAIN_EXPECTED,data("Hujan",WeatherForecast.FRESH_MILLIS-1).state(NOW));assertEquals(WeatherForecast.State.UNKNOWN,data("Hujan",WeatherForecast.FRESH_MILLIS).state(NOW));assertEquals(WeatherForecast.State.UNKNOWN,data("Hujan",-1).state(NOW));}
 @Test public void overnightUsesPreviousDatedNight()throws Exception{Instant early=Instant.parse("2026-10-23T18:00:00Z");WeatherForecast f=WeatherForecast.parse(raw("Tiada Hujan","Tiada Hujan","Hujan"),ID,early.toEpochMilli());assertEquals(WeatherForecast.State.RAIN_EXPECTED,f.state(early));assertEquals(LocalDate.of(2026,10,23),WeatherForecast.forecastDate(early.atZone(WeatherForecast.ZONE)));}
 @Test public void boundariesAreExplicitMalaysiaTime(){for(int[] pair:new int[][]{{5,2},{6,0},{11,0},{12,1},{18,1},{19,2}})assertEquals(pair[1],WeatherForecast.period(ZonedDateTime.of(2026,10,24,pair[0],0,0,0,WeatherForecast.ZONE)));assertEquals(Instant.parse("2026-10-24T11:00:00Z"),WeatherForecast.nextBoundary(NOW));}
 @Test public void missingDateHasNoInventedFallback()throws Exception{WeatherForecast f=data("Hujan",0);assertEquals(WeatherForecast.State.UNKNOWN,f.state(NOW.plus(Duration.ofDays(3))));}
 @Test public void malformedAndEmptyResponsesAreRejected()throws Exception{for(String raw:new String[]{"bad","[]"}){try{WeatherForecast.parse(raw,ID,NOW.toEpochMilli());fail("Invalid data accepted");}catch(Exception expected){}}}
 @Test public void duplicateAndWrongLocationRejected()throws Exception{String r=raw("Hujan","Hujan","Hujan");JSONArray a=new JSONArray(r);a.put(a.get(0));try{WeatherForecast.parse(a.toString(),ID,NOW.toEpochMilli());fail();}catch(IllegalArgumentException expected){}try{WeatherForecast.parse(r,"Ds001",NOW.toEpochMilli());fail();}catch(IllegalArgumentException expected){}}
 @Test public void queryIsBoundedAndLocationFiltered()throws Exception{String u=MetWeatherRepository.endpoint(ID);assertTrue(u.contains("Ds554%40location__location_id"));assertTrue(u.endsWith("limit=14"));assertFalse(u.contains("warning"));assertFalse(WeatherForecast.validId("Ds554/../secret"));}
 @Test public void retryAfterAndResponseLimitsAreBounded()throws Exception{assertEquals(120_000,MetWeatherRepository.retryDelay("120",NOW.toEpochMilli()));assertEquals(900_000,MetWeatherRepository.retryDelay(null,NOW.toEpochMilli()));assertEquals(86_400_000,MetWeatherRepository.retryDelay("99999999",NOW.toEpochMilli()));try{MetWeatherRepository.read(new ByteArrayInputStream(new byte[65537]));fail();}catch(IOException expected){}}
}
