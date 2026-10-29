package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** Two decorative sprite rivals. In-app only, silent, input-transparent and bounded to one safe lane. */
final class MiniMechaView extends View {
 static final String PREFS="mini-mecha";
 static final long LOOP=20_000;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final SharedPreferences prefs;
 private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),fx=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Rect source=new Rect();private final RectF target=new RectF();
 private Bitmap atlas;
 private boolean host,screen=true,obscured,attached,destroyed,scheduled,assetFailed;
 private long lastTick,playhead;long callbacks;
 private int interval=50;
 private final Runnable frame=()->{scheduled=false;callbacks++;if(!canRun())return;long now=SystemClock.elapsedRealtime();playhead=(playhead+Math.min(120,Math.max(0,now-lastTick)))%LOOP;lastTick=now;invalidate();schedule();};
 MiniMechaView(Context context){
  super(context);prefs=context.getSharedPreferences(PREFS,0);setTag("mini-mecha");
  setFocusable(false);setFocusableInTouchMode(false);setClickable(false);setLongClickable(false);
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);
  ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);if(manager!=null&&manager.isLowRamDevice())interval=100;
  fx.setStrokeCap(Paint.Cap.ROUND);
 }
 boolean featureEnabled(){return prefs.getBoolean("enabled",true);}
 void featureEnabled(boolean enabled){prefs.edit().putBoolean("enabled",enabled).apply();reconcile();}
 void hostActive(boolean active){host=active;reconcile();}
 void screen(boolean on){screen=on;reconcile();}
 void obscured(boolean value){obscured=value;reconcile();}
 private boolean motionAllowed(){return Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;}
 private boolean canRun(){return !destroyed&&attached&&host&&screen&&!obscured&&featureEnabled()&&motionAllowed()&&!assetFailed;}
 boolean running(){return canRun()&&scheduled;}
 boolean frameScheduled(){return scheduled;}
 long playhead(){return playhead;}
 int frameInterval(){return interval;}
 private void reconcile(){
  handler.removeCallbacks(frame);scheduled=false;
  if(!canRun()){setVisibility(GONE);releaseAtlas();return;}
  if(atlas==null){try(var raw=getContext().getAssets().open("mini-mecha/rivals.b64");var in=new android.util.Base64InputStream(raw,android.util.Base64.DEFAULT)){
    BitmapFactory.Options opts=new BitmapFactory.Options();opts.inScaled=false;atlas=BitmapFactory.decodeStream(in,null,opts);
    if(atlas==null||atlas.getWidth()!=512||atlas.getHeight()!=320)throw new java.io.IOException("Invalid sprite atlas");
   }catch(java.io.IOException|RuntimeException failure){assetFailed=true;releaseAtlas();setVisibility(GONE);return;}}
  setVisibility(VISIBLE);lastTick=SystemClock.elapsedRealtime();invalidate();schedule();
 }
 private void schedule(){if(scheduled||!canRun())return;scheduled=true;handler.postDelayed(frame,interval);}
 private void releaseAtlas(){if(atlas!=null){atlas.recycle();atlas=null;}}
 void destroy(){destroyed=true;handler.removeCallbacks(frame);scheduled=false;setVisibility(GONE);releaseAtlas();}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;reconcile();}
 @Override protected void onDetachedFromWindow(){attached=false;handler.removeCallbacks(frame);scheduled=false;releaseAtlas();super.onDetachedFromWindow();}
 static float whitePosition(long millis){float t=Math.floorMod(millis,LOOP)/1000f;if(t<5)return .08f+.47f*t/5;if(t<9)return .55f;if(t<14)return .55f-.47f*(t-9)/5;return .08f;}
 static boolean runningPhase(long millis){long t=Math.floorMod(millis,LOOP);return t<5000||t>=9000&&t<14000;}
 // Deterministic preview hook; never starts a timer or bypasses visibility/lifecycle gates.
 void previewAt(long millis){playhead=Math.floorMod(millis,LOOP);invalidate();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);if(atlas==null||destroyed)return;
  float unit=Math.min(getWidth()/880f,getHeight()/96f);if(unit<=0)return;
  canvas.save();canvas.scale(unit,unit);float width=getWidth()/unit,span=Math.max(0,width-88);
  float t=playhead/1000f;boolean run=runningPhase(playhead),right=t<9||t>=14;
  float a=14+whitePosition(playhead)*span,b=a+.27f*span;
  float bob=run?(float)Math.abs(Math.sin(playhead*Math.PI/180))*3:0;
  int pose=run?(int)(playhead/120)%4:0;
  robot(canvas,0,pose,a,12-bob,right||!run,run);
  robot(canvas,1,pose,b,12-(run?3-bob:0),run&&right,run);
  // Tiny non-flashing sci-fi beams only during a duel, never fullscreen flashes or audio.
  boolean duel=!run&&t<18;
  if(duel){float cycle=(playhead%1000)/1000f;boolean fromWhite=(playhead/1000)%2==0;
   float start=fromWhite?a+59:b+3,end=fromWhite?b+10:a+52;
   if(cycle<.72f){float head=start+(end-start)*cycle/.72f,tail=head-(fromWhite?1:-1)*18;
    fx.setColor(fromWhite?0xff7aeaff:0xffffc66d);fx.setStyle(Paint.Style.STROKE);fx.setStrokeWidth(2);canvas.drawLine(tail,52,head,52,fx);
   }else if(cycle<.9f){fx.setStyle(Paint.Style.FILL);fx.setColor(0xffffd989);canvas.drawCircle(end,52,3,fx);}
  }
  canvas.restore();
 }
 private void robot(Canvas c,int row,int pose,float x,float y,boolean facingRight,boolean run){
  source.set(pose*128,row*160,(pose+1)*128,(row+1)*160);target.set(x,y,x+61,y+76);
  // Native sheet: white faces right, crimson faces left.
  boolean flip=(row==0)!=facingRight;c.save();if(flip)c.scale(-1,1,x+30.5f,y+38);
  c.drawBitmap(atlas,source,target,spritePaint);c.restore();
 }
}
