package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Validated JAKIM data. All dates and countdowns use Malaysia time, never the TV timezone. */
final class PrayerTimes {
    static final ZoneId MALAYSIA = ZoneId.of("Asia/Kuala_Lumpur");
    static final String[] KEYS = {"fajr", "syuruk", "dhuhr", "asr", "maghrib", "isha"};
    static final String[] NAMES = {"Subuh", "Syuruk", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String[] MONTHS = {"jan","feb","mac","apr","mei","jun","jul","ogo","sep","okt","nov","dis"};
    static final class Day {
        final LocalDate date; final String hijri; final LocalTime[] times;
        Day(LocalDate d, String h, LocalTime[] t) { date=d; hijri=h; times=t.clone(); }
    }
    static final class Next {
        final int index; final ZonedDateTime at; final boolean tomorrow;
        Next(int i, ZonedDateTime t, boolean nextDay) { index=i; at=t; tomorrow=nextDay; }
        long minutesUntil(Instant now) { return Math.max(0, (Duration.between(now, at.toInstant()).getSeconds()+59)/60); }
    }
    static LocalDate parseDate(String value) {
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(value);
        String[] p=value.toLowerCase(Locale.ROOT).split("-");
        if(p.length!=3) throw new IllegalArgumentException("Invalid JAKIM date");
        int m=Arrays.asList(MONTHS).indexOf(p[1])+1;
        if(m==0) m=Arrays.asList("jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec").indexOf(p[1])+1;
        return LocalDate.of(Integer.parseInt(p[2]),m,Integer.parseInt(p[0]));
    }
    static Map<LocalDate,Day> parse(String raw, String zone, YearMonth month) throws Exception {
        JSONObject root=new JSONObject(raw);
        if(!"OK!".equals(root.optString("status")) || !zone.equals(root.optString("zone")))
            throw new IllegalArgumentException("JAKIM status/zone mismatch");
        JSONArray rows=root.getJSONArray("prayerTime");
        Map<LocalDate,Day> result=new TreeMap<>();
        for(int n=0;n<rows.length();n++) {
            JSONObject row=rows.getJSONObject(n);
            LocalDate date=parseDate(row.getString("date"));
            if(!YearMonth.from(date).equals(month)) throw new IllegalArgumentException("Wrong JAKIM month");
            LocalTime[] times=new LocalTime[KEYS.length];
            for(int i=0;i<times.length;i++) {
                times[i]=LocalTime.parse(row.getString(KEYS[i]));
                if(i>0 && !times[i].isAfter(times[i-1])) throw new IllegalArgumentException("Unordered prayer times");
            }
            if(result.put(date,new Day(date,row.optString("hijri"),times))!=null)
                throw new IllegalArgumentException("Duplicate JAKIM date");
        }
        if(result.isEmpty()) throw new IllegalArgumentException("Empty JAKIM schedule");
        return result;
    }
    static Next next(Map<LocalDate,Day> days, Instant instant) {
        ZonedDateTime now=instant.atZone(MALAYSIA);
        for(int offset=0;offset<2;offset++) {
            LocalDate date=now.toLocalDate().plusDays(offset);
            Day day=days.get(date); if(day==null) continue;
            for(int i=0;i<KEYS.length;i++) {
                if(i==1) continue; // Syuruk is displayed, but is not an obligatory prayer.
                ZonedDateTime at=ZonedDateTime.of(date,day.times[i],MALAYSIA);
                if(at.isAfter(now)) return new Next(i,at,offset>0);
            }
        }
        return null;
    }
    static final int[] PRAYERS={0,2,3,4,5};
    /** Schedule intervals, not a ruling on the end of a prayer's permitted time. */
    static int current(Day day, Instant instant) {
        if(day==null || !day.date.equals(instant.atZone(MALAYSIA).toLocalDate())) return -1;
        LocalTime now=instant.atZone(MALAYSIA).toLocalTime();
        if(now.isBefore(day.times[0])) return -1; // Never highlight today's future Isyak.
        if(now.isBefore(day.times[1])) return 0;
        if(now.isBefore(day.times[2])) return -1; // After Syuruk, Subuh is not current.
        for(int i=5;i>=2;i--) if(!now.isBefore(day.times[i])) return i;
        return -1;
    }
    static long nextTickMillis(Day day, Instant instant) {
        ZonedDateTime now=instant.atZone(MALAYSIA);
        Instant boundary=now.toLocalDate().plusDays(1).atStartOfDay(MALAYSIA).toInstant();
        if(day!=null && day.date.equals(now.toLocalDate())) for(LocalTime time:day.times) {
            Instant at=day.date.atTime(time).atZone(MALAYSIA).toInstant();
            if(at.isAfter(instant)&&at.isBefore(boundary)) boundary=at;
        }
        return Math.max(1,Math.min(30000,Duration.between(instant,boundary).toMillis()));
    }
    static String hhmm(LocalTime t) { return t.format(DateTimeFormatter.ofPattern("HH:mm",Locale.ROOT)); }
}
