package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.widget.ImageView;
import java.util.function.Supplier;

/** One outgoing + one incoming bitmap; latest-only pending loader never decodes intermediate states. */
final class WallpaperDissolveView extends ImageView {
    static final long DURATION_MS=800;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Drawable outgoing;
    private long started;
    private float fraction=1;
    private boolean active,closed;
    private String desired="",applied="",pendingKey;
    private Supplier<Bitmap> pending;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF outgoingBounds=new RectF();
    private final SharedPreferences motionPrefs;
    private final Runnable frame=new Runnable(){public void run(){
        if(closed||outgoing==null)return;
        fraction=motion()?Math.min(1,(SystemClock.uptimeMillis()-started)/(float)DURATION_MS):1;invalidate();
        if(fraction<1)handler.postDelayed(this,33);else{outgoing=null;drain();}
    }};
    WallpaperDissolveView(Context context){super(context);motionPrefs=RainOptions.prefs(context);setScaleType(ScaleType.CENTER_CROP);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    boolean animating(){return outgoing!=null;}
    String desiredKey(){return desired;}
    void request(String key,Supplier<Bitmap> loader){
        if(closed||key.equals(desired))return;desired=key;
        // The requested scene is already the incoming image. Cancel any superseded queue.
        // B -> C -> B while fading to B must not decode/fade B a second time.
        if(key.equals(applied)){pending=null;pendingKey=null;return;}
        if(outgoing!=null){pendingKey=key;pending=loader;return;}
        apply(key,loader);
    }
    private boolean motion(){return active&&motionPrefs.getBoolean("moving",true)&&Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;}
    private void apply(String key,Supplier<Bitmap> loader){
        Bitmap bitmap=loader.get();if(bitmap==null){desired=applied;return;}
        Drawable old=getDrawable();setImageBitmap(bitmap);applied=key;
        if(old!=null&&motion()){outgoing=old;fraction=0;started=SystemClock.uptimeMillis();handler.post(frame);}else{outgoing=null;fraction=1;}
        invalidate();
    }
    private void drain(){Supplier<Bitmap> next=pending;String key=pendingKey;pending=null;pendingKey=null;if(next!=null)apply(key,next);}
    void hostActive(boolean value){active=value;if(!value){handler.removeCallbacks(frame);outgoing=null;fraction=1;drain();invalidate();}}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(outgoing instanceof BitmapDrawable){
            Bitmap bitmap=((BitmapDrawable)outgoing).getBitmap();
            float scale=Math.max(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());
            float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
            paint.setAlpha(Math.round(255*(1-fraction)));
            outgoingBounds.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
            canvas.drawBitmap(bitmap,null,outgoingBounds,paint);
        }
    }
    void destroy(){closed=true;handler.removeCallbacksAndMessages(null);pending=null;outgoing=null;setImageDrawable(null);}
}
