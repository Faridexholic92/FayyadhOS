package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small resolution-independent line icons; no emoji/font or image-library dependency. */
final class PrayerGlyph extends View {
    private final int event;
    private final Path path=new Path();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public PrayerGlyph(Context context){this(context,2);}
    PrayerGlyph(Context context,int event){super(context);this.event=event;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);int saved=canvas.save();
        float unit=Math.min(getWidth(),getHeight())/32f;
        canvas.translate((getWidth()-32*unit)/2,(getHeight()-32*unit)/2);canvas.scale(unit,unit);
        paint.setColor(event==1||event==2?0xffffd46b:event==4?0xffff9658:GlassUi.CYAN);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setStrokeCap(Paint.Cap.ROUND);
        if(event<0){
            Path dome=path;dome.reset();dome.moveTo(6,28);dome.lineTo(6,17);dome.cubicTo(6,10,13,10,16,4);dome.cubicTo(19,10,26,10,26,17);dome.lineTo(26,28);canvas.drawPath(dome,paint);
            canvas.drawLine(3,29,29,29,paint);canvas.drawLine(16,1,16,4,paint);
            Path door=path;door.reset();door.moveTo(12,28);door.lineTo(12,22);door.quadTo(16,14,20,22);door.lineTo(20,28);canvas.drawPath(door,paint);
            canvas.drawLine(9,19,9,26,paint);canvas.drawLine(23,19,23,26,paint);
        }else if(event==5){
            // A closed crescent path avoids painting an opaque disc over the wallpaper.
            Path moon=path;moon.reset();moon.moveTo(19,3);moon.cubicTo(6,4,5,22,17,26);moon.cubicTo(22,28,27,25,29,21);moon.cubicTo(17,24,12,13,19,3);canvas.drawPath(moon,paint);
        }else{
            boolean horizon=event!=2;
            if(horizon){canvas.drawArc(9,12,23,26,180,180,false,paint);canvas.drawLine(3,20,29,20,paint);canvas.drawLine(7,24,25,24,paint);}
            else canvas.drawCircle(16,16,6,paint);
            float cy=horizon?19:16;
            for(int degrees=0;degrees<360;degrees+=45){if(horizon&&degrees>0&&degrees<180)continue;double a=Math.toRadians(degrees);canvas.drawLine(16+(float)Math.cos(a)*10,cy+(float)Math.sin(a)*10,16+(float)Math.cos(a)*13,cy+(float)Math.sin(a)*13,paint);}
        }
        canvas.restoreToCount(saved);
    }
}
