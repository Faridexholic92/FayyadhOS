package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import java.util.concurrent.*;

/** Decorative pre-rendered rigged 3D, not a real-time 3D engine. Silent and input-transparent. */
final class MiniMechaView extends View {
 interface FrameSource {MechaFrames load(Context context,boolean lowRam)throws Exception;}
 static final String PREFS="mini-mecha";
 static final long LOOP=20_000;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final SharedPreferences prefs;
 private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),fx=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Rect source=new Rect();private final RectF target=new RectF();
 private final FrameSource frameSource;private final boolean lowRam;
 private final ExecutorService loader=Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"mecha-atlas-loader");return thread;});
 private Future<?> loadTask;private MechaFrames frames;
 private boolean host,screen=true,obscured,attached,destroyed,scheduled,assetFailed,loading;
 private int loadGeneration;
 private long lastTick,playhead;long callbacks;
 private int interval=50;
 private final Runnable frame=()->{scheduled=false;callbacks++;if(!canRun()){reconcile();return;}long now=SystemClock.elapsedRealtime();playhead=(playhead+Math.min(120,Math.max(0,now-lastTick)))%LOOP;lastTick=now;invalidate();schedule();};
 MiniMechaView(Context context){this(context,MechaFrames::load);}
 MiniMechaView(Context context,FrameSource source){
  super(context);frameSource=source;prefs=context.getSharedPreferences(PREFS,0);setTag("mini-mecha");
  setFocusable(false);setFocusableInTouchMode(false);setClickable(false);setLongClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);
  ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);lowRam=manager!=null&&manager.isLowRamDevice();if(lowRam)interval=100;fx.setStrokeCap(Paint.Cap.ROUND);
 }
 boolean featureEnabled(){return prefs.getBoolean("enabled",true);}
 void featureEnabled(boolean enabled){prefs.edit().putBoolean("enabled",enabled).apply();if(enabled)assetFailed=false;reconcile();}
 String menuLabel(){return "Mini robot · Pra-render 3D · "+(featureEnabled()?"ON":"OFF")+(assetFailed?" · aset gagal":"");}
 void hostActive(boolean active){host=active;reconcile();}
 void screen(boolean on){screen=on;reconcile();}
 void obscured(boolean value){obscured=value;reconcile();}
 private boolean motionAllowed(){return Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;}
 private boolean allowed(){return !destroyed&&attached&&host&&screen&&!obscured&&featureEnabled()&&motionAllowed()&&!assetFailed;}
 private boolean canRun(){return allowed()&&frames!=null;}
 boolean running(){return canRun()&&scheduled;}
 boolean frameScheduled(){return scheduled;}
 boolean loading(){return loading;}
 boolean assetsReady(){return frames!=null;}
 boolean assetFailed(){return assetFailed;}
 long allocatedBytes(){return frames==null?0:frames.allocatedBytes();}
 long playhead(){return playhead;}
 int frameInterval(){return interval;}
 private void reconcile(){
  handler.removeCallbacks(frame);scheduled=false;
  if(!allowed()){cancelLoad();setVisibility(GONE);releaseFrames();return;}
  if(frames!=null){setVisibility(VISIBLE);lastTick=SystemClock.elapsedRealtime();invalidate();schedule();return;}
  setVisibility(GONE);if(loading)return;loading=true;int token=++loadGeneration;
  loadTask=loader.submit(()->{
   MechaFrames result=null;boolean failed=false;
   try{result=frameSource.load(getContext(),lowRam);}catch(Exception|OutOfMemoryError failure){failed=true;}
   MechaFrames answer=result;boolean problem=failed;
   handler.post(()->{
    if(token!=loadGeneration||!allowed()){if(answer!=null)answer.recycle();return;}
    loading=false;loadTask=null;
    if(problem||answer==null){assetFailed=true;setVisibility(GONE);if(answer!=null)answer.recycle();return;}
    frames=answer;setVisibility(VISIBLE);lastTick=SystemClock.elapsedRealtime();invalidate();schedule();
   });
  });
 }
 private void cancelLoad(){loadGeneration++;if(loadTask!=null)loadTask.cancel(true);loadTask=null;loading=false;}
 private void schedule(){if(scheduled||!canRun())return;scheduled=true;handler.postDelayed(frame,interval);}
 private void releaseFrames(){if(frames!=null){frames.recycle();frames=null;}}
 void destroy(){destroyed=true;cancelLoad();handler.removeCallbacks(frame);scheduled=false;setVisibility(GONE);releaseFrames();loader.shutdownNow();}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;reconcile();}
 @Override protected void onDetachedFromWindow(){attached=false;cancelLoad();handler.removeCallbacks(frame);scheduled=false;setVisibility(GONE);releaseFrames();super.onDetachedFromWindow();}
 static float whitePosition(long millis){float t=Math.floorMod(millis,LOOP)/1000f;if(t<5)return .08f+.47f*t/5;if(t<9)return .55f;if(t<14)return .55f-.47f*(t-9)/5;return .08f;}
 static boolean runningPhase(long millis){long t=Math.floorMod(millis,LOOP);return t<5000||t>=9000&&t<14000;}
 static int phaseAt(long millis){long t=Math.floorMod(millis,LOOP);return runningPhase(t)?0:t<18000?1:2;}
 static int frameAt(int row,long millis){long t=Math.floorMod(millis,LOOP);int phase=phaseAt(t);if(phase==0){long start=t<5000?0:9000,duration=row==0?1133:933;return (int)((t-start)%duration*32/duration);}long elapsed=phase==2?t-18000:t<9000?t-5000:t-14000;return (int)Math.min(phase==1?63:31,elapsed*(phase==1?64:32)/(phase==1?4000:2000));}
 void previewAt(long millis){playhead=Math.floorMod(millis,LOOP);invalidate();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);if(frames==null||destroyed)return;
  float unit=Math.min(getWidth()/880f,getHeight()/96f);if(unit<=0)return;
  canvas.save();canvas.scale(unit,unit);float span=Math.max(0,getWidth()/unit-112);
  float t=playhead/1000f;boolean run=runningPhase(playhead),right=t<9||t>=14;
  float a=14+whitePosition(playhead)*span,b=a+.27f*span;
  robot(canvas,0,a,4,right||!run);robot(canvas,1,b,4,run&&right);
  if(!run&&t<18){float cycle=(playhead%1000)/1000f;boolean fromAtlas=(playhead/1000)%2==0;float start=fromAtlas?a+65:b+12,end=fromAtlas?b+12:a+65;
   if(cycle<.72f){float head=start+(end-start)*cycle/.72f,tail=head-(fromAtlas?1:-1)*18;fx.setColor(fromAtlas?0xff7aeaff:0xffffc66d);fx.setStyle(Paint.Style.STROKE);fx.setStrokeWidth(2);canvas.drawLine(tail,56,head,56,fx);}
  }
  canvas.restore();
 }
 private void robot(Canvas c,int row,float x,float y,boolean facingRight){
  fx.setStyle(Paint.Style.FILL);fx.setColor(0x30081118);c.drawOval(x+14,87,x+64,92,fx);
  target.set(x,y,x+77,y+88);boolean flip=(row==0)!=facingRight;c.save();if(flip)c.scale(-1,1,x+38.5f,y+44);
  frames.draw(c,phaseAt(playhead),row,frameAt(row,playhead),source,target,spritePaint);c.restore();
 }
}
