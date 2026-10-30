package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Lightweight, static Kids Universe atmosphere drawn natively; no bitmap or animation allocation. */
final class KidsUniverseBackdrop extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    KidsUniverseBackdrop(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void circle(Canvas c, float x, float y, float radius, int color) {
        paint.setShader(null); paint.setColor(color); paint.setStyle(Paint.Style.FILL);
        c.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1f, radius * .08f));
        paint.setColor(0x66ffffff); c.drawCircle(x, y, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(), h=getHeight();
        if(w<=0||h<=0)return;
        // A subtle cool veil keeps white live text legible while allowing the wallpaper to show through.
        paint.setShader(new android.graphics.LinearGradient(0,0,w,h,
                new int[]{0x30234fbd,0x08234fbd,0x28201458},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint); paint.setShader(null);
        // Small fixed star field: intentionally static to keep TV CPU/GPU use low.
        int[] sx={.08f,.17f,.29f,.43f,.58f,.72f,.84f,.94f,.12f,.36f,.66f,.89f};
        int[] sy={.16f,.34f,.09f,.23f,.12f,.29f,.11f,.38f,.72f,.82f,.76f,.68f};
        for(int i=0;i<sx.length;i++){
            float r=(i%3==0?3.2f:1.8f)*getResources().getDisplayMetrics().density;
            paint.setColor(i%3==0?0xcffff0a8:0x99ffffff);
            canvas.drawCircle(w*sx[i],h*sy[i],r,paint);
        }
        float d=getResources().getDisplayMetrics().density;
        circle(canvas,w*.94f,h*.18f,31*d,0x55ffca55);
        circle(canvas,w*.055f,h*.73f,22*d,0x5548d9ff);
        // Layered rolling hills at the bottom edge, behind the app dock.
        path.reset();path.moveTo(0,h*.86f);path.cubicTo(w*.18f,h*.72f,w*.30f,h*.91f,w*.48f,h*.82f);
        path.cubicTo(w*.67f,h*.72f,w*.82f,h*.88f,w,h*.77f);path.lineTo(w,h);path.lineTo(0,h);path.close();
        paint.setColor(0x243a8de0);canvas.drawPath(path,paint);
        path.reset();path.moveTo(0,h*.93f);path.cubicTo(w*.22f,h*.83f,w*.36f,h*.98f,w*.57f,h*.89f);
        path.cubicTo(w*.78f,h*.80f,w*.88f,h*.94f,w,h*.87f);path.lineTo(w,h);path.lineTo(0,h);path.close();
        paint.setColor(0x263a42a5);canvas.drawPath(path,paint);
    }
}
