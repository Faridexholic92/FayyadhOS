package com.gathin.tvhome;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.View;
import android.widget.TextView;
final class GlassUi {
 static final int CYAN=0xff62e5ea,WHITE=0xfff5f8fb,MUTED=0xffc6d4df;
 static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static GradientDrawable surface(Context c,int fill,boolean focus) {
  GradientDrawable d=new GradientDrawable();
  d.setColor(fill);
  d.setCornerRadius(dp(c,14));d.setStroke(dp(c,focus?2:1),focus?CYAN:0x508facb9);return d;
 }
 static StateListDrawable states(Context c){return states(c,GlassMaterial.CONTROL);}
 static StateListDrawable tiles(Context c){return states(c,GlassMaterial.TILE);}
 private static StateListDrawable states(Context c,int kind){
  StateListDrawable d=new StateListDrawable();
  GradientDrawable disabled=surface(c,0xff152230,false);disabled.setStroke(dp(c,1),0x405e7487);
  d.addState(new int[]{-android.R.attr.state_enabled},disabled);
  d.addState(new int[]{android.R.attr.state_focused},new GlassMaterial(c,kind,true,false));
  d.addState(new int[]{android.R.attr.state_pressed},new GlassMaterial(c,kind,true,false));
  d.addState(new int[]{},new GlassMaterial(c,kind,false,false));return d;
 }
 static android.graphics.drawable.Drawable glow(Context c){return new GlassMaterial(c,GlassMaterial.CONTROL,true,false);}
 static void focus(View v){v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);v.setBackground(states(v.getContext()));}
 static void focusShortcut(View v){
  v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);StateListDrawable d=new StateListDrawable();
  d.addState(new int[]{android.R.attr.state_focused},glow(v.getContext()));d.addState(new int[]{android.R.attr.state_pressed},glow(v.getContext()));
  d.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));v.setBackground(d);
 }
 static void style(AlertDialog d){
  GlassWindows.apply(d);if(d.getWindow()!=null)d.getWindow().setDimAmount(0f);
  if(d.getListView()!=null){d.getListView().setSelector(glow(d.getContext()));d.getListView().setDrawSelectorOnTop(false);}
  for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL})
   if(d.getButton(id)!=null){d.getButton(id).setTextColor(WHITE);d.getButton(id).setBackground(states(d.getContext()));}
 }
 static TextView text(Context c,String value,float size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setShadowLayer(dp(c,2),0,dp(c,1),0xf0000000);t.setFontFeatureSettings("tnum");return t;}
}
