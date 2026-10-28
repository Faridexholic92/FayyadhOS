package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import androidx.recyclerview.widget.*;

/** Finite virtual runway, recentered invisibly at its edges. No duplicate package records. */
final class LoopCarousel extends RecyclerView {
    static final int REPEATS=201;
    private final LinearLayoutManager layout;
    private int pending=NO_POSITION;
    LoopCarousel(Context context){
        super(context);
        layout=new LinearLayoutManager(context,HORIZONTAL,false){
            @Override public void onLayoutCompleted(State state){super.onLayoutCompleted(state);fulfil();effects();}
        };
        setLayoutManager(layout);setItemAnimator(null);setOverScrollMode(OVER_SCROLL_NEVER);
        setClipChildren(false);setClipToPadding(false);
        addOnScrollListener(new OnScrollListener(){
            @Override public void onScrolled(RecyclerView view,int dx,int dy){effects();}
            @Override public void onScrollStateChanged(RecyclerView view,int state){if(state==SCROLL_STATE_IDLE)fulfil();}
        });
    }
    static int logical(int position,int size){return size==0?0:Math.floorMod(position,size);}
    static int anchor(int index,int size){return size<=1?index:(REPEATS/2)*size+index;}
    static int count(int size,boolean moving){return size<=1||moving?size:size*REPEATS;}
    void center(int position,boolean smooth){
        if(getAdapter()==null||position<0||position>=getAdapter().getItemCount())return;
        pending=position;
        if(smooth&&getWidth()>0){
            LinearSmoothScroller scroller=new LinearSmoothScroller(getContext()){
                @Override public int calculateDtToFit(int start,int end,int boxStart,int boxEnd,int preference){return (boxStart+boxEnd-start-end)/2;}
            };
            scroller.setTargetPosition(position);layout.startSmoothScroll(scroller);
        }else{
            android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
            int slot=GlassUi.dp(getContext(),110*Math.min(metrics.widthPixels/(metrics.density*960f),metrics.heightPixels/(metrics.density*540f)));
            // Actual decorated width is read after layout; this estimate only handles first layout.
            View sample=getChildCount()>0?getChildAt(0):null;
            if(sample!=null)slot=layout.getDecoratedMeasuredWidth(sample)+((LayoutParams)sample.getLayoutParams()).leftMargin+((LayoutParams)sample.getLayoutParams()).rightMargin;
            layout.scrollToPositionWithOffset(position,Math.max(0,(getWidth()-slot)/2));
        }
        post(this::fulfil);
    }
    private void fulfil(){
        if(pending==NO_POSITION)return;
        ViewHolder holder=findViewHolderForAdapterPosition(pending);
        if(holder!=null){pending=NO_POSITION;holder.itemView.requestFocus();}
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(pending!=NO_POSITION)center(pending,false);}
    void effects(){
        float center=getWidth()/2f;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);float slot=Math.max(1,child.getWidth()+GlassUi.dp(getContext(),10));
            float distance=Math.abs((child.getLeft()+child.getRight())/2f-center)/slot;
            float scale=distance<=1?1.2f-.15f*distance:distance<=2?1.05f-.1f*(distance-1):Math.max(.85f,.95f-.1f*(distance-2));
            child.setScaleX(scale);child.setScaleY(scale);child.setTranslationY(-GlassUi.dp(getContext(),2)*Math.max(0,1-distance));
        }
        invalidate();
    }
    @Override protected void dispatchDraw(Canvas canvas){
        // Actual edge mask: content fades to the wallpaper, not to a painted solid panel.
        int save=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
        super.dispatchDraw(canvas);
        if(getWidth()>0){
            Paint mask=new Paint();mask.setShader(new LinearGradient(0,0,getWidth(),0,new int[]{0x00ffffff,0xffffffff,0xffffffff,0x00ffffff},new float[]{0,.09f,.91f,1},Shader.TileMode.CLAMP));
            mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));canvas.drawRect(0,0,getWidth(),getHeight(),mask);
        }
        canvas.restoreToCount(save);
    }
}
