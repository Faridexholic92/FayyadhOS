package com.gathin.tvhome;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class PrayerTimesTest {
    private static PrayerTimes.Day day(LocalDate date) {
        return new PrayerTimes.Day(date,"1448-05-12",new LocalTime[]{
                LocalTime.of(5,14),LocalTime.of(6,23),LocalTime.of(12,27),
                LocalTime.of(15,44),LocalTime.of(18,28),LocalTime.of(19,38)});
    }
    private Map<LocalDate,PrayerTimes.Day> schedule(LocalDate date) {
        Map<LocalDate,PrayerTimes.Day> map=new TreeMap<>();
        map.put(date,day(date)); map.put(date.plusDays(1),day(date.plusDays(1))); return map;
    }
    private Instant at(String date, String time) {
        return LocalDateTime.parse(date+"T"+time).atZone(PrayerTimes.MALAYSIA).toInstant();
    }
    @Test public void malayMonthsAndEnglishMonths() {
        String[] ms={"Jan","Feb","Mac","Apr","Mei","Jun","Jul","Ogo","Sep","Okt","Nov","Dis"};
        for(int i=0;i<12;i++) assertEquals(LocalDate.of(2026,i+1,1),PrayerTimes.parseDate("01-"+ms[i]+"-2026"));
        assertEquals(LocalDate.of(2026,10,23),PrayerTimes.parseDate("23-Oct-2026"));
        assertEquals(LocalDate.of(2026,10,23),PrayerTimes.parseDate("2026-10-23"));
    }
    @Test public void beforeFajr() {
        PrayerTimes.Next n=PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),at("2026-10-23","04:00:00"));
        assertEquals(0,n.index); assertEquals(74,n.minutesUntil(at("2026-10-23","04:00:00"))); assertFalse(n.tomorrow);
    }
    @Test public void sunriseNotCountedAsPrayer() {
        assertEquals(2,PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),at("2026-10-23","06:00:00")).index);
    }
    @Test public void exactBoundaryAdvances() {
        assertEquals(3,PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),at("2026-10-23","12:27:00")).index);
    }
    @Test public void afterIshaUsesTomorrow() {
        PrayerTimes.Next n=PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),at("2026-10-23","23:00:00"));
        assertEquals(0,n.index); assertTrue(n.tomorrow);
        assertEquals(LocalDate.of(2026,10,24),n.at.toLocalDate());
    }
    @Test public void yearRollover() {
        PrayerTimes.Next n=PrayerTimes.next(schedule(LocalDate.of(2026,12,31)),at("2026-12-31","23:59:59"));
        assertEquals(LocalDate.of(2027,1,1),n.at.toLocalDate());
    }
    @Test public void neverReusesYesterdayAsToday() {
        Map<LocalDate,PrayerTimes.Day> m=new TreeMap<>(); m.put(LocalDate.of(2026,10,22),day(LocalDate.of(2026,10,22)));
        assertNull(PrayerTimes.next(m,at("2026-10-23","23:00:00")));
    }
    @Test public void tvTimezoneDoesNotChangePrayer() {
        TimeZone saved=TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            assertEquals(0,PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),Instant.parse("2026-10-22T20:00:00Z")).index);
        } finally { TimeZone.setDefault(saved); }
    }
    private String response(String zone,String date,String fajr) {
        return "{\"status\":\"OK!\",\"zone\":\""+zone+"\",\"prayerTime\":[{\"date\":\""+date+"\",\"hijri\":\"1448-05-12\","+
                "\"fajr\":\""+fajr+"\",\"syuruk\":\"06:23:00\",\"dhuhr\":\"12:27:00\",\"asr\":\"15:44:00\",\"maghrib\":\"18:28:00\",\"isha\":\"19:38:00\"}]}";
    }
    @Test public void parsesOfficialShape() throws Exception {
        Map<LocalDate,PrayerTimes.Day> m=PrayerTimes.parse(response("SWK08","23-Okt-2026","05:14:00"),"SWK08",YearMonth.of(2026,10));
        assertEquals("05:14",PrayerTimes.hhmm(m.get(LocalDate.of(2026,10,23)).times[0]));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongZone() throws Exception {
        PrayerTimes.parse(response("WLY01","23-Okt-2026","05:14:00"),"SWK08",YearMonth.of(2026,10));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsWrongMonth() throws Exception {
        PrayerTimes.parse(response("SWK08","23-Sep-2026","05:14:00"),"SWK08",YearMonth.of(2026,10));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsOutOfOrder() throws Exception {
        PrayerTimes.parse(response("SWK08","23-Okt-2026","18:00:00"),"SWK08",YearMonth.of(2026,10));
    }
    @Test(expected=Exception.class) public void rejectsHtmlError() throws Exception {
        PrayerTimes.parse("<html>Maintenance</html>","SWK08",YearMonth.of(2026,10));
    }
    @Test public void countdownRoundsUp() {
        PrayerTimes.Next n=PrayerTimes.next(schedule(LocalDate.of(2026,10,23)),at("2026-10-23","05:13:59"));
        assertEquals(1,n.minutesUntil(at("2026-10-23","05:13:59")));
    }
}
