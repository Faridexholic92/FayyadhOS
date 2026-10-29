package com.gathin.tvhome;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
public class SegmentedSpeedTest {
 @Test public void productionEndpointNeverRequests100MiB(){assertTrue(SpeedProbe.endpoint(SpeedProbe.CHUNK).endsWith("bytes=10485760"));assertThrows(IllegalArgumentException.class,()->SpeedProbe.endpoint(SpeedProbe.LIMIT));}
 @Test public void tenSegmentsRespectAggregateCapAndDisconnectEach()throws Exception{
  List<SpeedProbeTest.FakeConnection> connections=new ArrayList<>();long[] last={0};
  SpeedProbe probe=new SpeedProbe(()->{try{SpeedProbeTest.FakeConnection c=new SpeedProbeTest.FakeConnection();c.remaining=SpeedProbe.CHUNK;c.length=SpeedProbe.CHUNK;connections.add(c);return c;}catch(Exception e){throw new IOException(e);}},SpeedProbe.CHUNK);
  assertTrue(probe.run((bytes,time)->last[0]=bytes)>0);assertEquals(10,connections.size());assertEquals(SpeedProbe.LIMIT,last[0]);assertEquals(SpeedProbe.LIMIT,connections.stream().mapToLong(c->c.read).sum());assertTrue(connections.stream().allMatch(c->c.disconnected));assertTrue(connections.get(0).getRequestProperty("User-Agent").contains("FayyadhOS"));
 }
 @Test public void http403IsActionableAndNotReportedAsSpeed()throws Exception{SpeedProbeTest.FakeConnection c=new SpeedProbeTest.FakeConnection();c.code=403;c.length=SpeedProbe.CHUNK;Exception e=assertThrows(IOException.class,()->new SpeedProbe(()->c,SpeedProbe.CHUNK).run());assertTrue(SpeedProbe.error(e).contains("403"));assertEquals(0,c.read);assertTrue(c.disconnected);}
 @Test public void truncationInSecondSegmentNeverProducesFinalSuccess()throws Exception{AtomicInteger opens=new AtomicInteger();assertThrows(EOFException.class,()->new SpeedProbe(()->{try{SpeedProbeTest.FakeConnection c=new SpeedProbeTest.FakeConnection();c.length=SpeedProbe.CHUNK;c.remaining=opens.incrementAndGet()==1?SpeedProbe.CHUNK:1024;return c;}catch(Exception e){throw new IOException(e);}},SpeedProbe.CHUNK).run());assertEquals(2,opens.get());}
 @Test public void cancelBetweenSegmentsNeverOpensNext()throws Exception{AtomicInteger opens=new AtomicInteger();SpeedProbe probe=new SpeedProbe(()->{opens.incrementAndGet();try{SpeedProbeTest.FakeConnection c=new SpeedProbeTest.FakeConnection();c.length=SpeedProbe.CHUNK;c.remaining=SpeedProbe.CHUNK;return c;}catch(Exception e){throw new IOException(e);}},SpeedProbe.CHUNK);probe.cancel();assertThrows(InterruptedIOException.class,probe::run);assertEquals(0,opens.get());}
 @Test public void errorsSeparateTlsDnsAndTimeoutWithoutDisablingSsl(){assertTrue(SpeedProbe.error(new javax.net.ssl.SSLHandshakeException("bad")).contains("TLS"));assertTrue(SpeedProbe.error(new UnknownHostException()).contains("DNS"));assertTrue(SpeedProbe.error(new SocketTimeoutException()).contains("masa"));}
}
