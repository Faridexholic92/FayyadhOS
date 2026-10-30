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
    private final Path bolt=new Path(),waterPath=new Path(),trailPath=new Path();
    private final Paint lensPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Matrix lensMatrix=new Matrix();
    private Bitmap backdrop;private BitmapShader lensShader;private boolean capturing;
    private final float[] travel=new float[MAX_MOVING];
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
                float stepDistance=velocity[i]*speed*elapsed/1000f;
                y[i]+=stepDistance;travel[i]=Math.min(radius[i]*11f,travel[i]+stepDistance);
                x[i]+=(float)Math.sin(y[i]*.055f+i*2.1f)*stepDistance*.07f;
                if(y[i]>worldHeight+radius[i]*2){y[i]=175f;x[i]=random.nextFloat()*worldWidth;travel[i]=0;}
                // Rare, bounded merging with only the handful of larger stationary drops.
                for(int j=MAX_MOVING;j<Math.min(9,count);j++){
                    float dx=x[i]-x[j],dy=y[i]-y[j],distance=radius[i]+radius[j];
                    if(dx*dx+dy*dy<distance*distance*.3f){radius[i]=Math.min(18,(float)Math.sqrt(radius[i]*radius[i]+radius[j]*radius[j]*.5f));x[j]=random.nextFloat()*worldWidth;y[j]=175f+random.nextFloat()*Math.max(1,worldHeight-175f);break;}
                }
            }
        }
        invalidate();schedule();
    }
    private void finishFade(){cancel();fade=0;setVisibility(GONE);releaseSprites();}
    private void prepare(){
        if(getWidth()>0){unit=Math.max(.1f,Math.min(getWidth()/960f,getHeight()/540f));worldWidth=getWidth()/unit;worldHeight=getHeight()/unit;}
        if(!seeded){for(int i=0;i<count;i++){x[i]=random.nextFloat()*worldWidth;y[i]=175f+random.nextFloat()*Math.max(1,worldHeight-175f);if(x[i]<320&&y[i]<390)y[i]=390f+random.nextFloat()*Math.max(1,worldHeight-390f);radius[i]=i<MAX_MOVING?9+random.nextFloat()*5:i<14?4+random.nextFloat()*4:1.2f+random.nextFloat()*2.6f;velocity[i]=12+random.nextFloat()*15;kind[i]=i<MAX_MOVING?2:i<9?1:0;}seeded=true;}
        if(bolt.isEmpty()){bolt.moveTo(755,34);bolt.lineTo(720,92);bolt.lineTo(746,86);bolt.lineTo(715,148);bolt.lineTo(777,71);bolt.lineTo(750,78);bolt.close();bolt.offset(65,110);}
        for(int i=0;i<streakCount();i++)if(rainSpeed[i]==0){rainX[i]=random.nextFloat()*worldWidth;rainY[i]=random.nextFloat()*worldHeight;rainSpeed[i]=100+random.nextFloat()*65;}
        if(sprites[0]==null){for(int i=0;i<3;i++)sprites[i]=makeSprite(i);}
        if(backdrop==null)captureBackdrop();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);seeded=false;
        if(hostActive&&wanted){prepare();invalidate();}
    }
    /** One bounded scene capture per activation, not per-frame readback or live blur. */
    private void captureBackdrop(){
        if(!(getParent() instanceof View)||getWidth()==0)return;
        View root=(View)getParent();if(root.getWidth()==0||root.getHeight()==0)return;
        int w=Math.min(960,root.getWidth()),h=Math.max(1,Math.round(w*root.getHeight()/(float)root.getWidth()));
        backdrop=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(backdrop);c.scale(w/(float)root.getWidth(),h/(float)root.getHeight());
        capturing=true;try{root.draw(c);}finally{capturing=false;}
        lensShader=new BitmapShader(backdrop,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);lensPaint.setShader(lensShader);
    }
    int backdropBytes(){return backdrop==null?0:backdrop.getAllocationByteCount();}
    private static Bitmap makeSprite(int kind){
        int w=96,h=kind==2?128:96;Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);RectF r=new RectF(9,9,w-9,h-9);Path shape=new Path();
        if(kind==2){shape.moveTo(47,9);shape.cubicTo(58,32,86,65,84,91);shape.cubicTo(85,125,14,125,12,92);shape.cubicTo(10,64,39,32,47,9);shape.close();}
        else shape.addOval(r,Path.Direction.CW);
        p.setShader(new RadialGradient(43,h*.43f,49,new int[]{0x00dcecff,0x00dcecff,0x3a102430,0x55071119},new float[]{0,.65f,.89f,1},Shader.TileMode.CLAMP));c.drawPath(shape,p);
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.1f);p.setColor(0x7c09151d);c.drawPath(shape,p);
        // Broken Fresnel glints rather than a complete cartoon bubble outline.
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(2.2f);p.setColor(0xeee6f5ff);c.drawArc(r,209,37,false,p);
        p.setStrokeWidth(.9f);p.setColor(0x70ffdeb0);c.drawArc(r,39,49,false,p);
        p.setStrokeWidth(2.8f);p.setColor(0x95f4fbff);c.drawArc(new RectF(15,15,w-15,h-15),215,14,false,p);
        return b;
    }
    private void drawLens(Canvas c,int i,float r,float ry){
        if(lensShader==null)return;
        waterPath.reset();
        if(i<MAX_MOVING){waterPath.moveTo(x[i],y[i]-ry);waterPath.cubicTo(x[i]+r*.3f,y[i]-ry*.35f,x[i]+r,y[i],x[i]+r*.95f,y[i]+ry*.35f);waterPath.cubicTo(x[i]+r,y[i]+ry,x[i]-r,y[i]+ry,x[i]-r*.95f,y[i]+ry*.35f);waterPath.cubicTo(x[i]-r,y[i],x[i]-r*.3f,y[i]-ry*.35f,x[i],y[i]-ry);waterPath.close();}
        else waterPath.addOval(x[i]-r,y[i]-ry,x[i]+r,y[i]+ry,Path.Direction.CW);
        float zoom=1.14f+Math.min(.12f,r*.009f),sx=worldWidth/backdrop.getWidth(),sy=worldHeight/backdrop.getHeight();
        lensMatrix.setScale(sx*zoom,sy*zoom);lensMatrix.postTranslate(x[i]*(1-zoom)+r*.18f,y[i]*(1-zoom)-ry*.13f);
        lensShader.setLocalMatrix(lensMatrix);lensPaint.setAlpha(Math.round(255*fade*(.55f+opacity*.3f)));c.drawPath(waterPath,lensPaint);
    }
    private void drawTrail(Canvas c,int i,float r){
        float length=travel[i];if(length<r)return;
        float bottom=y[i]-r*.5f,top=bottom-length,w=Math.max(.45f,r*.13f),bend=(float)Math.sin(y[i]*.025f+i)*r*.32f;
        trailPath.reset();trailPath.moveTo(x[i]-w,bottom);trailPath.cubicTo(x[i]+bend-w,bottom-length*.4f,x[i]-bend,top+length*.2f,x[i],top);
        trailPath.cubicTo(x[i]-bend+w,top+length*.2f,x[i]+bend+w,bottom-length*.4f,x[i]+w,bottom);trailPath.close();
        trailPaint.setStyle(Paint.Style.FILL);trailPaint.setColor(0xff102735);trailPaint.setAlpha(Math.round(68*opacity*fade));c.drawPath(trailPath,trailPaint);
        trailPaint.setStyle(Paint.Style.STROKE);trailPaint.setStrokeWidth(.42f);trailPaint.setColor(0xffe5f4fa);trailPaint.setAlpha(Math.round(90*opacity*fade));c.drawPath(trailPath,trailPaint);
    }
    @Override protected void onDraw(Canvas canvas){
        if(capturing||!hostActive||fade<=0||sprites[0]==null)return;
        long start=System.nanoTime();drawCount++;
        int saved=canvas.save();canvas.scale(unit,unit);
        bitmapPaint.setAlpha(Math.round(255*(.65f+opacity*.35f)*fade));int m=movingCount();
        for(int i=0;i<count;i++){
            float r=radius[i];
            if(i<m)drawTrail(canvas,i,r);
            float ry=kind[i]==2?r*1.32f:r;drawLens(canvas,i,r,ry);
            destination.set(x[i]-r,y[i]-ry,x[i]+r,y[i]+ry);
            canvas.drawBitmap(sprites[kind[i]],null,destination,bitmapPaint);
        }
        rainPaint.setColor(0xffcbefff);rainPaint.setAlpha(Math.round(45*opacity*fade));rainPaint.setStrokeWidth(.45f);
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
    private void releaseSprites(){if(backdrop!=null){backdrop.recycle();backdrop=null;}lensPaint.setShader(null);lensShader=null;for(int i=0;i<sprites.length;i++)if(sprites[i]!=null){sprites[i].recycle();sprites[i]=null;}}
    void destroy(){hostActive=false;wanted=false;cancel();releaseSprites();}
    boolean hasScheduledFrame(){return scheduled;}
    boolean effectVisible(){return getVisibility()==VISIBLE&&fade>0;}
    int dropletCount(){return count;}
    int activeMovingCount(){return movingCount();}
    int spriteBytes(){int total=0;for(Bitmap b:sprites)if(b!=null)total+=b.getAllocationByteCount();return total;}
}
