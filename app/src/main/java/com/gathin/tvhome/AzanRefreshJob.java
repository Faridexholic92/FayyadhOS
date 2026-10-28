package com.gathin.tvhome;

import android.app.job.*;
import android.os.Handler;
import java.time.*;
import java.util.*;

/** Network-constrained refresh while opted in; periodic and recovery jobs have independent lifecycles. */
public final class AzanRefreshJob extends JobService {
 private final Map<Integer, Work> running = new HashMap<>();
 private final class Work implements Runnable {
  final JobParameters params;
  final Thread thread;
  volatile boolean stopped;
  Work(JobParameters p) { params=p;thread=new Thread(this,"azan-cache-refresh-"+p.getJobId()); }
  void cancel() { stopped=true;thread.interrupt(); }
  @Override public void run() {
   boolean retry=false;
   try {
    String zone=AzanScheduler.zone(AzanRefreshJob.this);
    LocalDate today=LocalDate.now(PrayerTimes.MALAYSIA);
    PrayerRepository repo=new PrayerRepository(AzanRefreshJob.this);
    repo.fetch(zone,YearMonth.from(today));
    if(!stopped && today.getDayOfMonth()>=28)repo.fetch(zone,YearMonth.from(today).plusMonths(1));
   } catch(Exception e) {
    retry=true;
    if(!stopped)AzanStore.note(AzanRefreshJob.this,"Kemas kini jadual gagal; cache sah dikekalkan.");
   }
   final boolean again=retry;
   new Handler(getMainLooper()).post(()->{
    // Stopping one job must not suppress completion of a different job in the same service.
    if(stopped || running.get(params.getJobId())!=this)return;
    running.remove(params.getJobId());
    AzanScheduler.reconcile(AzanRefreshJob.this);
    jobFinished(params,again);
   });
  }
 }
 @Override public boolean onStartJob(JobParameters p) {
  if(!AzanStore.enabled(this) || !AzanScheduler.problem(this).isEmpty())return false;
  Work old=running.remove(p.getJobId());if(old!=null)old.cancel();
  Work work=new Work(p);running.put(p.getJobId(),work);work.thread.start();return true;
 }
 @Override public boolean onStopJob(JobParameters p) {
  Work work=running.remove(p.getJobId());if(work!=null)work.cancel();
  return AzanStore.enabled(this);
 }
 @Override public void onDestroy() {
  for(Work work:running.values())work.cancel();running.clear();super.onDestroy();
 }
}
