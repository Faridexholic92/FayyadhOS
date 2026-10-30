package com.gathin.tvhome;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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
 static StateListDrawable kidsStates(Context c) {
  StateListDrawable d=new StateListDrawable();
  GradientDrawable focused=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xf52b91d9,0xf20a3265});
  focused.setCornerRadius(dp(c,16)); focused.setStroke(dp(c,3),0xff65e8ff);
  GradientDrawable normal=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xe91a2946,0xe90a152d});
  normal.setCornerRadius(dp(c,16)); normal.setStroke(dp(c,1),0x9976b9ed);
  GradientDrawable pressed=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xf02b6595,0xf0142b50});
  pressed.setCornerRadius(dp(c,16)); pressed.setStroke(dp(c,2),0xff8cecff);
  d.addState(new int[]{android.R.attr.state_focused},focused);
  d.addState(new int[]{android.R.attr.state_pressed},pressed);
  d.addState(new int[]{},normal); return d;
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
 static Typeface typeface(Context c,int style){boolean bold=(style&Typeface.BOLD)!=0;Typeface base=c.getResources().getFont(bold?R.font.poppins_bold:R.font.poppins_regular);return Typeface.create(base,style&Typeface.ITALIC);}
 static TextView text(Context c,String value,float size,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(typeface(c,Typeface.NORMAL));t.setIncludeFontPadding(false);t.setShadowLayer(dp(c,2),0,dp(c,1),0xa8000000);t.setFontFeatureSettings("tnum");return t;}
}
