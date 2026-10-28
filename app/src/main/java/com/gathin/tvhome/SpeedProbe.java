package com.gathin.tvhome;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit, single-stream download only. Bounded memory/data; never uploads or starts itself. */
final class SpeedProbe {
 static final int LIMIT=100*1024*1024;
 static final int DEADLINE_SECONDS=120;
 interface Progress { void update(long bytes,double seconds); }
 interface ConnectionFactory { HttpsURLConnection open() throws IOException; }
 private final ConnectionFactory factory;
 private volatile boolean cancelled;private volatile HttpsURLConnection connection;
 SpeedProbe(){this(()->(HttpsURLConnection)new URL("https://speed.cloudflare.com/__down?bytes="+LIMIT).openConnection());}
 SpeedProbe(ConnectionFactory factory){this.factory=factory;}
 static double mbps(long bytes,double seconds){if(bytes<=0||seconds<=0||!Double.isFinite(seconds))throw new IllegalArgumentException("Invalid sample");return bytes*8.0/seconds/1000000.0;}
 double run() throws Exception {return run((bytes,seconds)->{});}
 double run(Progress progress) throws Exception {
  checkCancelled();HttpsURLConnection c=factory.open();connection=c;
  ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();AtomicBoolean expired=new AtomicBoolean();
  ScheduledFuture<?> deadline=timer.schedule(()->{expired.set(true);c.disconnect();},DEADLINE_SECONDS,TimeUnit.SECONDS);
  long count=0,start=System.nanoTime(),last=start;
  try{
   c.setConnectTimeout(10000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
   c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("Cache-Control","no-cache");
   checkCancelled();
   if(c.getResponseCode()!=200)throw new IOException("Download endpoint unavailable");
   String type=c.getContentType();if(type!=null&&(type.toLowerCase(Locale.ROOT).contains("text/")||type.toLowerCase(Locale.ROOT).contains("json")))throw new IOException("Unexpected download response");
   String encoding=c.getContentEncoding();if(encoding!=null&&!encoding.equalsIgnoreCase("identity"))throw new IOException("Unexpected encoded response");
   long length=c.getContentLengthLong();if(length>=0&&length!=LIMIT)throw new IOException("Unexpected download length");
   // Elapsed time includes connection/TLS/response setup: an honest end-to-end estimate, not link speed.
   try(InputStream in=c.getInputStream()){
    byte[] buffer=new byte[65536];
    while(count<LIMIT){
     checkCancelled();if(expired.get())throw new IOException("Download timed out");
     int n=in.read(buffer,0,(int)Math.min(buffer.length,LIMIT-count));
     if(n<0)throw new EOFException("Incomplete download");if(n==0)continue;count+=n;
     long now=System.nanoTime();if(now-last>=250_000_000L){progress.update(count,(now-start)/1e9);last=now;}
    }
   }
   checkCancelled();if(expired.get())throw new IOException("Download timed out");
   double seconds=(System.nanoTime()-start)/1e9;progress.update(count,seconds);checkCancelled();return mbps(count,seconds);
  }finally{deadline.cancel(false);timer.shutdownNow();c.disconnect();connection=null;}
 }
 private void checkCancelled() throws InterruptedIOException {if(cancelled||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");}
 void cancel(){cancelled=true;HttpsURLConnection c=connection;if(c!=null)c.disconnect();}
}
