package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Resolution-independent lettering. Never crops or keys a screenshot of the brand. */
final class FayyadhWordmarkView extends View {
    private final Path lettering=new Path();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final float artWidth,artHeight;
    private final Shader colours;
    FayyadhWordmarkView(Context context){
        super(context);setTag("fayyadhos-logo");setContentDescription("FayyadhOS");
        setFocusable(false);setClickable(false);
        paint.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD_ITALIC));paint.setTextSize(100f);
        paint.getTextPath("FayyadhOS",0,9,0,0,lettering);lettering.computeBounds(bounds,true);
        lettering.offset(12f-bounds.left,12f-bounds.top);lettering.computeBounds(bounds,true);
        artWidth=bounds.right+22f;artHeight=bounds.bottom+22f;
        colours=new LinearGradient(12,0,artWidth-22f,0,new int[]{0xffff7355,0xffffd247,0xffffab28,0xfff65b30,0xff55e9ff,0xff169ce8},new float[]{0,.23f,.46f,.70f,.79f,1},Shader.TileMode.CLAMP);
    }
    float artworkWidth(){return artWidth;}
    float artworkHeight(){return artHeight;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(getWidth()==0||getHeight()==0)return;
        float fit=Math.min(getWidth()/artWidth,getHeight()/artHeight);
        int save=canvas.save();canvas.translate(0,(getHeight()-artHeight*fit)/2f);canvas.scale(fit,fit);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);
        // Clean, bounded blue depth and cyan edge; all paths stay inside the view.
        for(int d=9;d>=1;d--){int layer=canvas.save();canvas.translate(d*.6f,d);paint.setStrokeWidth(3.4f);paint.setColor(0xff072b73);canvas.drawPath(lettering,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(d>4?0xff06377e:0xff0b51a0);canvas.drawPath(lettering,paint);paint.setStyle(Paint.Style.STROKE);canvas.restoreToCount(layer);}
        paint.setStrokeWidth(3f);paint.setColor(0xff67dfff);canvas.drawPath(lettering,paint);
        paint.setStyle(Paint.Style.FILL);paint.setShader(colours);canvas.drawPath(lettering,paint);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.7f);paint.setColor(0xb8fff5c4);canvas.drawPath(lettering,paint);
        canvas.restoreToCount(save);
    }
}
