package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class AdaptiveSpeedRecoveryTest {
 static class Slow extends SpeedProbeTest.FakeConnection {
  final AtomicLong nanos;Slow(AtomicLong n)throws Exception{nanos=n;length=remaining=SpeedProbe.CHUNK;}
  public InputStream getInputStream(){InputStream base=super.getInputStream();return new InputStream(){public int read()throws IOException{return base.read();}public int read(byte[] b,int off,int len)throws IOException{int n=base.read(b,off,len);nanos.addAndGet(TimeUnit.SECONDS.toNanos(1));return n;}};}
 }
 @Test public void slowConnectionReturnsHonestPartialSampleInsteadOfRequiring100MiB()throws Exception{
  AtomicLong time=new AtomicLong();Slow c=new Slow(time);AtomicInteger opens=new AtomicInteger();long[] progress={0};
  SpeedProbe p=new SpeedProbe(()->{opens.incrementAndGet();return c;},SpeedProbe.CHUNK,time::get);
  double result=p.run((bytes,seconds)->progress[0]=bytes);
  assertEquals(655360,c.read);assertEquals(c.read,p.sampleBytes());assertEquals(c.read,progress[0]);assertEquals(10,p.sampleSeconds(),0);
  assertEquals(SpeedProbe.mbps(c.read,10),result,0.000001);assertEquals(1,opens.get());assertTrue(c.read<SpeedProbe.CHUNK);assertTrue(c.disconnected);
 }
 @Test public void elapsedDeadlineBeforeEnoughDataIsFailureAndDisconnects()throws Exception{
  AtomicLong time=new AtomicLong();Slow c=new Slow(time){public InputStream getInputStream(){InputStream base=super.getInputStream();return new InputStream(){public int read()throws IOException{return base.read();}public int read(byte[] b,int off,int len)throws IOException{int n=base.read(b,off,len);time.addAndGet(TimeUnit.SECONDS.toNanos(120));return n;}};}};
  SpeedProbe p=new SpeedProbe(()->c,SpeedProbe.CHUNK,time::get);Exception e=assertThrows(SpeedProbe.DeadlineFailure.class,p::run);
  assertTrue(SpeedProbe.error(e).contains("120"));assertEquals(0,p.sampleBytes());assertTrue(c.disconnected);
 }
 @Test public void cancelledPartialSampleCannotBecomeSuccess()throws Exception{
  AtomicLong time=new AtomicLong();Slow c=new Slow(time);SpeedProbe p=new SpeedProbe(()->c,SpeedProbe.CHUNK,time::get);
  assertThrows(InterruptedIOException.class,()->p.run((bytes,seconds)->p.cancel()));assertEquals(0,p.sampleBytes());assertTrue(c.disconnected);
 }
}
