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
 static StateListDrawable states(Context c) {
  StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},glow(c));
  d.addState(new int[]{android.R.attr.state_pressed},surface(c,0xd0245360,true));
  d.addState(new int[]{},surface(c,0xad10212e,false));return d;
 }
 static android.graphics.drawable.Drawable glow(Context c){
  GradientDrawable halo=surface(c,0x2036def5,false);halo.setStroke(dp(c,5),0x5536def5);
  android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{halo,surface(c,0xd0245360,true)});
  layers.setLayerInset(1,dp(c,2),dp(c,2),dp(c,2),dp(c,2));return layers;
 }
 static StateListDrawable premiumStates(Context c){
  StateListDrawable states=new StateListDrawable();
  GradientDrawable selected=surface(c,0x303f7495,false);selected.setStroke(dp(c,1),0xffb6dff8);selected.setCornerRadius(dp(c,7));
  GradientDrawable idle=surface(c,0x251a3045,false);idle.setStroke(dp(c,1),0x557c9eb5);idle.setCornerRadius(dp(c,7));
  states.addState(new int[]{android.R.attr.state_focused},selected);states.addState(new int[]{android.R.attr.state_pressed},selected);states.addState(new int[]{},idle);return states;
 }
 static void focus(View v){v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);v.setBackground(states(v.getContext()));}
 static void focusShortcut(View v){
  v.setFocusable(true);v.setFocusableInTouchMode(true);v.setClickable(true);
  StateListDrawable states=new StateListDrawable();
  states.addState(new int[]{android.R.attr.state_focused},surface(v.getContext(),0xd0245360,true));
  states.addState(new int[]{android.R.attr.state_pressed},surface(v.getContext(),0xd0245360,true));
  GradientDrawable clear=new GradientDrawable();clear.setColor(Color.TRANSPARENT);states.addState(new int[]{},clear);v.setBackground(states);
 }
 static void style(AlertDialog d){
  if(d.getWindow()!=null){d.getWindow().setBackgroundDrawable(surface(d.getContext(),0xff122432,false));d.getWindow().setDimAmount(0f);}
  if(d.getListView()!=null){d.getListView().setSelector(surface(d.getContext(),0x90428591,true));}
  for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL})
   if(d.getButton(id)!=null)d.getButton(id).setTextColor(CYAN);
 }
 static TextView text(Context c,String value,float size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setShadowLayer(dp(c,2),0,dp(c,1),0xf0000000);t.setFontFeatureSettings("tnum");return t;}
}
