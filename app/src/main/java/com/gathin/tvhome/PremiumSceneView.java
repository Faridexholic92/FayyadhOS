package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.View;

/** Native bounded sprite renderer: 12 predecoded frames, no video, blur, physics or network. */
final class PremiumSceneView extends View {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Rect src=new Rect();private final RectF dest=new RectF();
    private final WallpaperDissolveView wallpaper;
    private Bitmap atlas;
    private boolean active,interactive=true,obscured,audioBusy,enabled=true,birthday,closed,scheduled;
    private int quality=2;private long started=SystemClock.uptimeMillis(),elapsed,partyStarted;
    private final Runnable frame=()->{scheduled=false;if(!canAnimate())return;elapsed=SystemClock.uptimeMillis()-started;invalidate();drift();schedule();};
    PremiumSceneView(Context c,WallpaperDissolveView wallpaper){
        super(c);this.wallpaper=wallpaper;setTag("premium-scene");setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        try(java.io.InputStream in=c.getAssets().open("premium/heroes-run.webp")){atlas=BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}
    }
    void configure(boolean enabled,boolean birthday,int quality){
        if(birthday&&!this.birthday)partyStarted=SystemClock.uptimeMillis();
        this.enabled=enabled;this.birthday=birthday;this.quality=quality;sync();
    }
    void hostActive(boolean value){if(value&&!active)started=SystemClock.uptimeMillis()-elapsed;active=value;sync();}
    void screen(boolean value){interactive=value;sync();}
    void obscured(boolean value){obscured=value;sync();}
    void audioBusy(boolean value){audioBusy=value;sync();}
    boolean canAnimate(){return !closed&&enabled&&active&&interactive&&!obscured&&!audioBusy&&quality>0
        &&RainOptions.prefs(getContext()).getBoolean("moving",true)
        &&Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;}
    boolean hasFrame(){return scheduled;}
    private void sync(){main.removeCallbacks(frame);scheduled=false;if(!canAnimate())resetDrift();else schedule();invalidate();}
    private void schedule(){if(!scheduled&&canAnimate()){scheduled=true;main.postDelayed(frame,quality==1?100:50);}}
    private void drift(){if(quality<2)return;wallpaper.setScaleX(1.018f);wallpaper.setScaleY(1.018f);wallpaper.setTranslationX((float)Math.sin(elapsed/16000.0)*getWidth()*.003f);}
    private void resetDrift(){wallpaper.setScaleX(1);wallpaper.setScaleY(1);wallpaper.setTranslationX(0);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(!enabled||closed)return;
        int save=c.save();c.scale(getWidth()/960f,getHeight()/540f);
        if(birthday){drawConfetti(c);c.restoreToCount(save);return;}
        if(atlas==null||audioBusy){c.restoreToCount(save);return;}
        // All action remains in the empty middle: never over the prayer/header or app controls.
        float phase=(elapsed%18000)/1000f;boolean animated=canAnimate();
        float approach=phase<4?phase/4:phase<7?1:phase<11?1-(phase-7)/4:0;
        if(!animated)approach=.45f;
        int cell=animated&&phase<11?(int)(elapsed/100)%6:0;
        float papa=345+approach*85,boy=650-approach*110;
        float hop=animated&&phase>=4&&phase<7?(float)Math.abs(Math.sin(phase*6))*10:0;
        // Source frames face right; mirror BoBoiBoy toward PapaZola. Turn both on retreat.
        sprite(c,0,cell,papa,396-hop,206,phase>=7&&phase<11&&animated);
        sprite(c,1,cell,boy,399-hop*.65f,156,!(phase>=7&&phase<11&&animated));
        if(animated&&phase>=4&&phase<7){
            paint.setColor(0xa7f5d07d);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.3f);
            c.drawArc(461,299-hop,500,338-hop,phase*110,140,false,paint);paint.setStyle(Paint.Style.FILL);
        }
        c.restoreToCount(save);
    }
    private void sprite(Canvas c,int row,int cell,float x,float ground,float height,boolean mirror){
        src.set(cell*192,row*320,(cell+1)*192,(row+1)*320);
        float width=height*192/320f;dest.set(x-width/2,ground-height,x+width/2,ground);
        int save=c.save();if(mirror)c.scale(-1,1,x,ground-height/2);paint.setAlpha(255);c.drawBitmap(atlas,src,dest,paint);c.restoreToCount(save);
    }
    private void drawConfetti(Canvas c){
        if(!canAnimate())return;long age=SystemClock.uptimeMillis()-partyStarted;if(age>10000)return;
        for(int i=0;i<18;i++){
            float x=48+(i*131%864),y=((age*.045f+i*37)%290)+180;
            paint.setColor(i%2==0?0xa8edc975:0x99bedff1);c.drawRect(x,y,x+3,y+6,paint);
        }
    }
    @Override protected void onDetachedFromWindow(){main.removeCallbacks(frame);scheduled=false;resetDrift();super.onDetachedFromWindow();}
    void destroy(){closed=true;main.removeCallbacksAndMessages(null);scheduled=false;resetDrift();atlas=null;}
}
