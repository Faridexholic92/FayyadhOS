package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
import javax.net.ssl.*;import java.security.cert.Certificate;import java.net.*;import java.io.*;import java.util.concurrent.atomic.AtomicInteger;
public class SpeedProbeTest {
 static class FakeConnection extends HttpsURLConnection {
  long remaining=SpeedProbe.LIMIT,read;boolean disconnected;int code=200;String type="application/octet-stream",encoding;long length=SpeedProbe.LIMIT;
  FakeConnection()throws Exception{super(new URL("https://fixture.invalid/download"));}
  public void connect(){}public boolean usingProxy(){return false;}public void disconnect(){disconnected=true;}
  public int getResponseCode(){return code;}public String getContentType(){return type;}public String getContentEncoding(){return encoding;}public long getContentLengthLong(){return length;}
  public InputStream getInputStream(){return new InputStream(){public int read(){if(remaining<=0)return -1;remaining--;read++;return 0;}public int read(byte[] b,int off,int len){if(remaining<=0)return -1;int n=(int)Math.min(remaining,len);remaining-=n;read+=n;return n;}};}
  public String getCipherSuite(){return "FIXTURE";}public Certificate[] getLocalCertificates(){return null;}public Certificate[] getServerCertificates(){return null;}
 }
 @Test public void exact100MiBCapAndFinalProgress()throws Exception{FakeConnection c=new FakeConnection();c.remaining+=999;long[] last={0};double result=new SpeedProbe(()->c).run((bytes,seconds)->last[0]=bytes);assertEquals(104857600,c.read);assertEquals(SpeedProbe.LIMIT,last[0]);assertTrue(result>0);assertTrue(c.disconnected);assertFalse(c.getUseCaches());assertFalse(c.getInstanceFollowRedirects());assertEquals("GET",c.getRequestMethod());assertEquals("identity",c.getRequestProperty("Accept-Encoding"));}
 @Test public void cancelBeforeOpenDoesNotMakeRequest()throws Exception{AtomicInteger calls=new AtomicInteger();SpeedProbe p=new SpeedProbe(()->{calls.incrementAndGet();throw new IOException();});p.cancel();assertThrows(InterruptedIOException.class,()->p.run());assertEquals(0,calls.get());}
 @Test public void shortReadIsFailureNotSuccess()throws Exception{FakeConnection c=new FakeConnection();c.remaining=16384;assertThrows(EOFException.class,()->new SpeedProbe(()->c).run());assertTrue(c.disconnected);}
 @Test public void wrongStatusHtmlEncodingAndLengthAreRejected()throws Exception{for(int kind=0;kind<4;kind++){FakeConnection c=new FakeConnection();if(kind==0)c.code=503;if(kind==1)c.type="text/html";if(kind==2)c.encoding="gzip";if(kind==3)c.length=123;assertThrows(IOException.class,()->new SpeedProbe(()->c).run());assertEquals(0,c.read);assertTrue(c.disconnected);}}
 @Test public void cancellationDuringProgressStopsRead()throws Exception{FakeConnection c=new FakeConnection();SpeedProbe p=new SpeedProbe(()->c);assertThrows(InterruptedIOException.class,()->p.run((bytes,seconds)->p.cancel()));assertTrue(c.disconnected);}
 @Test public void invalidThroughputIsRejected(){assertThrows(IllegalArgumentException.class,()->SpeedProbe.mbps(1,Double.NaN));assertEquals(8,SpeedProbe.mbps(1_000_000,1),0.00001);}
}
