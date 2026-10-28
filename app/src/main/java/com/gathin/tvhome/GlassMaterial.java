package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Native approximation of Glassy UI's dark tint, lit rim and top sheen. No bitmap capture or render loop. */
final class GlassMaterial extends Drawable {
    static final int CONTROL=0,TILE=1,DOCK=2,PANEL=3;
    private final Paint fill=new Paint(3),sheen=new Paint(3),rim=new Paint(3),inner=new Paint(3);
    private final RectF area=new RectF(),inside=new RectF();
    private final float radius,stroke;private final boolean solid,focused;private final int kind;private final boolean blurred;
    private int alpha=255;
    GlassMaterial(Context c,int kind,boolean focused,boolean blurred){
        this.kind=kind;this.focused=focused;this.blurred=blurred;solid=GlassOptions.mode(c).equals("solid");
        android.util.DisplayMetrics m=c.getResources().getDisplayMetrics();float scale=Math.min(m.widthPixels/(m.density*960f),m.heightPixels/(m.density*540f));
        radius=GlassUi.dp(c,(kind==PANEL||kind==DOCK?22:16)*scale);stroke=Math.max(1,GlassUi.dp(c,(focused?2:1)*scale));
        rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(stroke);inner.setStyle(Paint.Style.STROKE);inner.setStrokeWidth(Math.max(1,GlassUi.dp(c,.5f)));
    }
    @Override protected void onBoundsChange(Rect b){
        float inset=stroke/2;area.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);inside.set(area);inside.inset(stroke,stroke);
        int a=solid||focused?255:kind==PANEL?(blurred?211:244):kind==DOCK?215:235;
        int top=focused?0xff284d68:Color.argb(a,34,46,61),bottom=focused?0xff19374d:Color.argb(a,13,23,35);
        fill.setShader(solid?null:new LinearGradient(0,area.top,0,area.bottom,top,bottom,Shader.TileMode.CLAMP));fill.setColor(focused?0xff20425b:0xff172332);
        sheen.setShader(new LinearGradient(area.left,area.top,area.right,Math.max(area.top+1,area.bottom),new int[]{0x1affffff,0x06ffffff,0x00ffffff},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(area.left,area.top,area.right,area.bottom,focused?new int[]{0xffb3edff,0xff7bd7ff,0xff4b91c0}:new int[]{0x99e4f2ff,0x37c4d5e8,0x57dceafa},new float[]{0,.58f,1},Shader.TileMode.CLAMP));
        inner.setColor(focused?0x804d9fc9:0x60050c17);
    }
    @Override public void draw(Canvas c){
        if(area.isEmpty())return;fill.setAlpha(alpha);sheen.setAlpha(alpha);rim.setAlpha(alpha);inner.setAlpha(alpha);
        c.drawRoundRect(area,radius,radius,fill);
        if(!solid)c.drawRoundRect(area,radius,radius,sheen);
        c.drawRoundRect(area,radius,radius,rim);c.drawRoundRect(inside,Math.max(0,radius-stroke),Math.max(0,radius-stroke),inner);
    }
    @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);}
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public int getAlpha(){return alpha;}
    @Override public void setColorFilter(ColorFilter filter){fill.setColorFilter(filter);sheen.setColorFilter(filter);rim.setColorFilter(filter);inner.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
