package com.gathin.tvhome;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit single-stream segmented download. Total data/time bounded; no upload or auto-run. */
final class SpeedProbe {
 static final int LIMIT=100*1024*1024,CHUNK=10*1024*1024;
 static final int DEADLINE_SECONDS=120;
 static final long SAMPLE_NANOS=10_000_000_000L,MIN_SAMPLE_BYTES=256*1024;
 interface Progress {void update(long bytes,double seconds);}
 interface ConnectionFactory {HttpsURLConnection open()throws IOException;}
 private final ConnectionFactory factory;private final int chunk;private final java.util.function.LongSupplier nanoTime;
 private volatile long sampleBytes;private volatile double sampleSeconds;
 long sampleBytes(){return sampleBytes;}double sampleSeconds(){return sampleSeconds;}
 private volatile boolean cancelled;private volatile HttpsURLConnection connection;
 SpeedProbe(){this(()->(HttpsURLConnection)new URL(endpoint(CHUNK)).openConnection(),CHUNK);}
 // Retain the full-response injection contract for earlier deterministic fixtures.
 SpeedProbe(ConnectionFactory factory){this(factory,LIMIT);}
 SpeedProbe(ConnectionFactory factory,int chunk){this(factory,chunk,System::nanoTime);}
 SpeedProbe(ConnectionFactory factory,int chunk,java.util.function.LongSupplier nanoTime){if(chunk<=0||LIMIT%chunk!=0)throw new IllegalArgumentException("Invalid chunk");this.factory=factory;this.chunk=chunk;this.nanoTime=nanoTime;}
 static String endpoint(int bytes){if(bytes<=0||bytes>CHUNK)throw new IllegalArgumentException("Request too large");return "https://speed.cloudflare.com/__down?bytes="+bytes;}
 static double mbps(long bytes,double seconds){if(bytes<=0||seconds<=0||!Double.isFinite(seconds))throw new IllegalArgumentException("Invalid sample");return bytes*8.0/seconds/1000000.0;}
 double run()throws Exception{return run((bytes,seconds)->{});}
 double run(Progress progress)throws Exception{
  checkCancelled();ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();AtomicBoolean expired=new AtomicBoolean();
  ScheduledFuture<?> deadline=timer.schedule(()->{expired.set(true);HttpsURLConnection c=connection;if(c!=null)c.disconnect();},DEADLINE_SECONDS,TimeUnit.SECONDS);
  sampleBytes=0;sampleSeconds=0;long count=0,start=nanoTime.getAsLong(),last=start;byte[] buffer=new byte[65536];
  try{
   download: while(count<LIMIT){
    checkCancelled();if(expired.get()||nanoTime.getAsLong()-start>=TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS))throw new DeadlineFailure();
    HttpsURLConnection c=factory.open();connection=c;
    try{
     c.setConnectTimeout(12_000);c.setReadTimeout(15_000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
     c.setRequestMethod("GET");c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("Cache-Control","no-cache");
     c.setRequestProperty("User-Agent","FayyadhOS/2.11.1 (Android TV; explicit download probe)");
     checkCancelled();int status=c.getResponseCode();if(status!=200)throw new EndpointFailure(status);
     String type=c.getContentType();if(type!=null&&(type.toLowerCase(Locale.ROOT).contains("text/")||type.toLowerCase(Locale.ROOT).contains("json")))throw new IOException("Respons bukan data download (captive portal / endpoint)");
     String encoding=c.getContentEncoding();if(encoding!=null&&!encoding.equalsIgnoreCase("identity"))throw new IOException("Respons dimampatkan; ukuran dibatalkan");
     long length=c.getContentLengthLong();if(length>=0&&length!=chunk)throw new IOException("Saiz respons tidak sepadan dengan segmen");
     long received=0;
     try(InputStream in=c.getInputStream()){
      while(received<chunk){
       checkCancelled();if(expired.get()||nanoTime.getAsLong()-start>=TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS))throw new DeadlineFailure();
       int n=in.read(buffer,0,(int)Math.min(buffer.length,chunk-received));
       if(n<0)throw new EOFException("Download terputus sebelum segmen lengkap");if(n==0)continue;
       count+=n;received+=n;long now=nanoTime.getAsLong();
       if(now-last>=250_000_000L){progress.update(count,(now-start)/1e9);last=now;}
       checkCancelled();if(expired.get()||now-start>=TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS))throw new DeadlineFailure();
       if(count>=MIN_SAMPLE_BYTES&&now-start>=SAMPLE_NANOS)break download;
      }
     }
    }finally{c.disconnect();connection=null;}
   }
   checkCancelled();if(expired.get()||nanoTime.getAsLong()-start>=TimeUnit.SECONDS.toNanos(DEADLINE_SECONDS))throw new DeadlineFailure();
   double seconds=(nanoTime.getAsLong()-start)/1e9;progress.update(count,seconds);checkCancelled();sampleBytes=count;sampleSeconds=seconds;return mbps(count,seconds);
  }catch(IOException failure){if(expired.get())throw new DeadlineFailure();throw failure;}finally{deadline.cancel(false);timer.shutdownNow();HttpsURLConnection c=connection;if(c!=null)c.disconnect();connection=null;}
 }
 static String error(Exception e){
  if(e instanceof EndpointFailure)return e.getMessage();
  if(e instanceof DeadlineFailure||"Had masa keseluruhan 120 saat dicapai".equals(e.getMessage()))return "Had masa keseluruhan 120 saat dicapai. Tiada sampel download mencukupi; cuba semula.";
  if(e instanceof javax.net.ssl.SSLException)return "TLS/certificate gagal. Semak tarikh TV dan kemas kini sistem; SSL tidak dipintas.";
  if(e instanceof java.net.UnknownHostException)return "DNS endpoint gagal. Semak Internet / DNS TV.";
  if(e instanceof java.net.SocketTimeoutException)return "Sambungan / bacaan tamat masa. Cuba semula apabila rangkaian stabil.";
  if(e instanceof InterruptedIOException)return "Ujian dibatalkan / sambungan terganggu.";
  if(e instanceof EOFException)return "Download tidak lengkap; tiada keputusan kelajuan disahkan.";
  return "Endpoint / rangkaian gagal. Cuba semula; semak Internet atau captive portal.";
 }
 static final class DeadlineFailure extends InterruptedIOException {DeadlineFailure(){super("Had masa keseluruhan 120 saat dicapai");}}
 static final class EndpointFailure extends IOException {
  final int status;EndpointFailure(int status){super("HTTP "+status+(status==403?" · endpoint menolak permintaan; cuba rangkaian lain / kemudian.":status==429?" · had permintaan; tunggu sebelum cuba semula.":" · endpoint tidak tersedia."));this.status=status;}
 }
 private void checkCancelled()throws InterruptedIOException{if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
 void cancel(){cancelled=true;HttpsURLConnection c=connection;if(c!=null)c.disconnect();}
}
