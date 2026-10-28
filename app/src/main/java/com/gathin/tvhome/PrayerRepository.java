package com.gathin.tvhome;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** Official JAKIM HTTPS only; validated monthly schedules, daily MYT revalidation. */
final class PrayerRepository {
    private static final Object CACHE_WRITE_LOCK=new Object();
    private final File directory;
    private final Clock clock;
    interface Fetcher { String request(String zone,YearMonth month)throws Exception; }
    private final Fetcher fetcher;
    PrayerRepository(Context context) { this(context,Clock.systemUTC()); }
    PrayerRepository(Context context,Clock clock) {this(context,clock,PrayerRepository::request);}
    PrayerRepository(Context context,Clock clock,Fetcher fetcher) { this.fetcher=fetcher;this.clock=clock;directory=new File(context.getFilesDir(),"jakim-esolat-v1"); }
    static boolean fresh(long stamp,Instant now) {
        return stamp>0 && stamp<=now.toEpochMilli() && Instant.ofEpochMilli(stamp).atZone(PrayerTimes.MALAYSIA).toLocalDate()
                .equals(now.atZone(PrayerTimes.MALAYSIA).toLocalDate());
    }
    private File cache(String zone, YearMonth month) {
        if(!zone.matches("[A-Z]{3}[0-9]{2}")) throw new IllegalArgumentException("Invalid zone");
        return new File(directory,zone+"-"+month+".json");
    }
    Map<java.time.LocalDate,PrayerTimes.Day> cached(String zone, YearMonth month) {
        try { return PrayerTimes.parse(read(new FileInputStream(cache(zone,month))),zone,month); }
        catch(Exception unavailable) { return Collections.emptyMap(); }
    }
    long updatedAt(String zone,YearMonth month) {return cache(zone,month).lastModified();}
    Map<java.time.LocalDate,PrayerTimes.Day> fetch(String zone, YearMonth month) throws Exception {return fetch(zone,month,false);}
    Map<java.time.LocalDate,PrayerTimes.Day> fetch(String zone,YearMonth month,boolean force) throws Exception {
        File file=cache(zone,month);
        if(!force && file.isFile() && fresh(file.lastModified(),clock.instant())) {
            Map<LocalDate,PrayerTimes.Day> saved=cached(zone,month);if(!saved.isEmpty()) return saved;
        }
        String raw=fetcher.request(zone,month);
        Map<LocalDate,PrayerTimes.Day> parsed=PrayerTimes.parse(raw,zone,month);
        if(parsed.size()!=month.lengthOfMonth())throw new IOException("Incomplete JAKIM month");
        // Cache only a fully validated response, replacing atomically.
        synchronized(CACHE_WRITE_LOCK) {
        if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cache unavailable");
        File tmp=new File(file.getPath()+".tmp");
        try(FileOutputStream out=new FileOutputStream(tmp)) { out.write(raw.getBytes(StandardCharsets.UTF_8)); out.getFD().sync(); }
        if(!tmp.renameTo(file)) { tmp.delete(); throw new IOException("Could not save JAKIM cache"); }
        file.setLastModified(clock.millis());
        // Retain previous/current/following month only. Old dates are never rendered as today.
        YearMonth current=YearMonth.from(clock.instant().atZone(PrayerTimes.MALAYSIA));
        File[] old=directory.listFiles((d,name)->name.endsWith(".json") &&
                !name.endsWith(current+".json") && !name.endsWith(current.minusMonths(1)+".json") && !name.endsWith(current.plusMonths(1)+".json"));
        if(old!=null) for(File stale:old) stale.delete();
        return parsed;
        }
    }
    private static String request(String zone,YearMonth month)throws Exception {
        String endpoint="https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=duration&zone="+zone;
        byte[] form=("datestart="+month.atDay(1)+"&dateend="+month.atEndOfMonth()).getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection c=(HttpsURLConnection)new URL(endpoint).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(12000); c.setInstanceFollowRedirects(false);
        c.setRequestMethod("POST");c.setDoOutput(true);c.setFixedLengthStreamingMode(form.length);
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        c.setRequestProperty("Accept","application/json");
        String raw;
        try {
            try(OutputStream out=c.getOutputStream()){out.write(form);}
            if(c.getResponseCode()!=200) throw new IOException("JAKIM HTTP "+c.getResponseCode());
            raw=read(c.getInputStream());
        } finally { c.disconnect(); }
        return raw;
    }
    private static String read(InputStream stream) throws IOException {
        try(InputStream in=stream; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192]; int n;
            while((n=in.read(b))!=-1) {
                if(out.size()+n>256*1024) throw new IOException("JAKIM response too large");
                out.write(b,0,n);
            }
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
}
