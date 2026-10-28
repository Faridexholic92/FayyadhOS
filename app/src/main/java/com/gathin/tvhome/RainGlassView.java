package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import java.util.Random;

/** In-app, input-transparent glass. Fixed arrays, tiny shared sprites, no frame-time allocation. */
final class RainGlassView extends View {
    static final int MAX_DROPS=96,MAX_MOVING=3,MAX_STREAKS=48;
    private final float[] rainX=new float[MAX_STREAKS],rainY=new float[MAX_STREAKS],rainSpeed=new float[MAX_STREAKS];
    private final Paint rainPaint=new Paint(Paint.ANTI_ALIAS_FLAG),boltPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bolt=new Path();
    private boolean thunder,lightningAllowed,reducedMotion;
    private long nextBolt,boltStart;
    private float boltStrength;
    void setThunder(boolean value){if(thunder==value)return;thunder=value;boltStrength=0;boltStart=0;nextBolt=SystemClock.elapsedRealtime()+18000;invalidate();}
    boolean thunderEnabled(){return thunder&&lightningAllowed&&!reducedMotion&&movingCount()>0;}
    int streakCount(){return Math.min(MAX_STREAKS,count/2);}
    private final float[] x=new float[MAX_DROPS],y=new float[MAX_DROPS],radius=new float[MAX_DROPS],velocity=new float[MAX_DROPS];
    private final int[] kind=new int[MAX_DROPS];
    private final Bitmap[] sprites=new Bitmap[3];
    private final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint trailPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destination=new RectF();
    private final Random random=new Random();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable frame=this::step;
    private boolean hostActive,wanted,scheduled,seeded;
    private int count=48,moving=2,qualityLevel,slowFrames;
    private long lastTick,dueAt;
    private float fade,opacity=.48f,speed=.6f,unit=1,worldWidth=960,worldHeight=540;
    // Package-visible counters support deterministic lifecycle tests, not an always-on profiler.
    long drawCount,frameCallbacks,lastDrawNanos;
    int frameInterval=50;

    public RainGlassView(Context context){
        super(context);setFocusable(false);setFocusableInTouchMode(false);setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
    }
    void configure(RainOptions options){
        reducedMotion=android.provider.Settings.Global.getFloat(getContext().getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0;
        lightningAllowed=options.lightning;
        int n=options.count();int m=options.moving&&!reducedMotion?(n>=96?3:2):0;
        boolean changed=n!=count||m!=moving||opacity!=options.opacity()||speed!=options.velocity();
        count=n;moving=m;opacity=options.opacity();speed=options.velocity();
        if(changed){seeded=false;qualityLevel=0;slowFrames=0;frameInterval=50;if(hostActive&&wanted){prepare();invalidate();schedule();}}
    }
    void setHostActive(boolean active){
        if(hostActive==active)return;hostActive=active;
        if(!active){cancel();fade=0;boltStrength=0;boltStart=0;setVisibility(GONE);releaseSprites();}
        else if(wanted){nextBolt=SystemClock.elapsedRealtime()+18000;prepare();setVisibility(VISIBLE);lastTick=SystemClock.elapsedRealtime();schedule();}
    }
    void setRain(boolean active){
        if(wanted==active)return;wanted=active;
        if(!hostActive)return;
        if(active){prepare();setVisibility(VISIBLE);lastTick=SystemClock.elapsedRealtime();schedule();}
        else if(fade>0)schedule();else finishFade();
    }
    private void schedule(){
        if(scheduled||!hostActive||(!wanted&&fade<=0))return;
        if(wanted&&fade>=1&&movingCount()==0)return;
        scheduled=true;dueAt=SystemClock.elapsedRealtime()+frameInterval;handler.postDelayed(frame,frameInterval);
    }
    private void cancel(){handler.removeCallbacks(frame);scheduled=false;lastTick=0;}
    private void step(){
        scheduled=false;frameCallbacks++;
        if(!hostActive)return;
        long now=SystemClock.elapsedRealtime();float elapsed=Math.min(120,Math.max(1,now-lastTick));lastTick=now;
        if(now-dueAt>120)noteCost(5_000_000L);
        fade=Math.max(0,Math.min(1,fade+(wanted?1:-1)*elapsed/700f));
        if(fade<=0&&!wanted){finishFade();return;}
        if(wanted){
            if(movingCount()>0)for(int i=0;i<streakCount();i++){
                rainY[i]+=rainSpeed[i]*speed*elapsed/1000f;rainX[i]-=8*speed*elapsed/1000f;
                if(rainY[i]>worldHeight+20){rainY[i]=-20;rainX[i]=random.nextFloat()*worldWidth;}
                if(rainX[i]<0)rainX[i]=worldWidth;
            }
            if(thunderEnabled()){
                if(now>=nextBolt){boltStart=now;nextBolt=now+18000+random.nextInt(12000);}
                float age=now-boltStart;boltStrength=boltStart>0&&age<420?(float)Math.sin(Math.PI*age/420f):0;
            }else boltStrength=0;
            int m=movingCount();
            for(int i=0;i<m;i++){
                y[i]+=velocity[i]*speed*elapsed/1000f;
                if(y[i]>worldHeight+radius[i]*2){y[i]=-radius[i]*2;x[i]=random.nextFloat()*worldWidth;}
                // Rare, bounded merging with only the handful of larger stationary drops.
                for(int j=MAX_MOVING;j<Math.min(9,count);j++){
                    float dx=x[i]-x[j],dy=y[i]-y[j],distance=radius[i]+radius[j];
                    if(dx*dx+dy*dy<distance*distance*.3f){radius[i]=Math.min(12,(float)Math.sqrt(radius[i]*radius[i]+radius[j]*radius[j]*.5f));x[j]=random.nextFloat()*worldWidth;y[j]=random.nextFloat()*worldHeight;break;}
                }
            }
        }
        invalidate();schedule();
    }
    private void finishFade(){cancel();fade=0;setVisibility(GONE);releaseSprites();}
    private void prepare(){
        if(getWidth()>0){unit=Math.max(.1f,Math.min(getWidth()/960f,getHeight()/540f));worldWidth=getWidth()/unit;worldHeight=getHeight()/unit;}
        if(!seeded){for(int i=0;i<count;i++){x[i]=random.nextFloat()*worldWidth;y[i]=random.nextFloat()*worldHeight;radius[i]=i<9?5+random.nextFloat()*4:1.8f+random.nextFloat()*2.7f;velocity[i]=2+random.nextFloat()*3;kind[i]=i<MAX_MOVING?2:i<9?1:0;}seeded=true;}
        if(bolt.isEmpty()){bolt.moveTo(755,34);bolt.lineTo(720,92);bolt.lineTo(746,86);bolt.lineTo(715,148);bolt.lineTo(777,71);bolt.lineTo(750,78);bolt.close();bolt.offset(65,110);}
        for(int i=0;i<streakCount();i++)if(rainSpeed[i]==0){rainX[i]=random.nextFloat()*worldWidth;rainY[i]=random.nextFloat()*worldHeight;rainSpeed[i]=100+random.nextFloat()*65;}
        if(sprites[0]==null){for(int i=0;i<3;i++)sprites[i]=makeSprite(i);}
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);seeded=false;
        if(hostActive&&wanted){prepare();invalidate();}
    }
    private static Bitmap makeSprite(int kind){
        int w=96,h=kind==2?128:96;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);RectF r=new RectF(8,8,w-8,h-8);Path shape=new Path();
        if(kind==2){shape.moveTo(48,8);shape.cubicTo(72,39,91,65,84,96);shape.cubicTo(76,129,18,126,11,96);shape.cubicTo(3,65,27,37,48,8);shape.close();}
        else shape.addOval(r,Path.Direction.CW);
        p.setShader(new RadialGradient(45,h*.49f,43,new int[]{0x00ffffff,0x02d7f1ff,0x244b7186,0x48071018},new float[]{0,.62f,.90f,1},Shader.TileMode.CLAMP));c.drawPath(shape,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.25f);p.setColor(0x78698593);c.drawPath(shape,p);
        p.setColor(0xc4f0fbff);p.setStrokeWidth(2.4f);c.drawArc(r,202,67,false,p);
        p.setColor(0x65d3f2ff);p.setStrokeWidth(2);c.drawArc(r,25,58,false,p);
        p.setStyle(Paint.Style.FILL);p.setColor(0xbaffffff);c.drawOval(27,18,34,22,p);
        return b;
    }
    @Override protected void onDraw(Canvas canvas){
        if(!hostActive||fade<=0||sprites[0]==null)return;
        long start=System.nanoTime();drawCount++;
        int saved=canvas.save();canvas.scale(unit,unit);
        bitmapPaint.setAlpha(Math.round(255*opacity*fade));int m=movingCount();
        for(int i=0;i<count;i++){
            float r=radius[i];
            if(i<m){trailPaint.setColor(0x45d8efff);trailPaint.setAlpha(Math.round(45*opacity*fade));trailPaint.setStrokeWidth(Math.max(.45f,r*.10f));canvas.drawLine(x[i],y[i]-r*3.8f,x[i],y[i]-r*.6f,trailPaint);}
            float ry=kind[i]==2?r*1.32f:r;
            destination.set(x[i]-r,y[i]-ry,x[i]+r,y[i]+ry);
            canvas.drawBitmap(sprites[kind[i]],null,destination,bitmapPaint);
        }
        rainPaint.setColor(0xffcbefff);rainPaint.setAlpha(Math.round(95*opacity*fade));rainPaint.setStrokeWidth(.65f);
        for(int i=0;i<streakCount();i++)canvas.drawLine(rainX[i],rainY[i],rainX[i]+1.2f,rainY[i]-9, rainPaint);
        // Local, low-contrast bolt only. Never flash the full television white.
        if(boltStrength>0&&thunderEnabled()){boltPaint.setColor(0xffd4ecff);boltPaint.setAlpha(Math.round(145*boltStrength*fade));canvas.drawPath(bolt,boltPaint);}
        canvas.restoreToCount(saved);lastDrawNanos=System.nanoTime()-start;
        if(m>0)noteCost(lastDrawNanos);
    }
    private void noteCost(long nanos){
        slowFrames=nanos>4_000_000L?slowFrames+1:Math.max(0,slowFrames-1);
        if(slowFrames>=10){qualityLevel=Math.min(2,qualityLevel+1);frameInterval=100;slowFrames=0;}
    }
    private int movingCount(){return qualityLevel>=2?0:qualityLevel==1?Math.min(1,moving):moving;}
    private void releaseSprites(){for(int i=0;i<sprites.length;i++)if(sprites[i]!=null){sprites[i].recycle();sprites[i]=null;}}
    void destroy(){hostActive=false;wanted=false;cancel();releaseSprites();}
    boolean hasScheduledFrame(){return scheduled;}
    boolean effectVisible(){return getVisibility()==VISIBLE&&fade>0;}
    int dropletCount(){return count;}
    int activeMovingCount(){return movingCount();}
    int spriteBytes(){int total=0;for(Bitmap b:sprites)if(b!=null)total+=b.getAllocationByteCount();return total;}
}
