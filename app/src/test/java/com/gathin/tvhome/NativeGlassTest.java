package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowDialog;
import java.io.*;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeGlassTest {
 final DashboardTest helper=new DashboardTest();
 Context context(){return RuntimeEnvironment.getApplication();}
 void mode(String mode){context().getSharedPreferences(GlassOptions.PREFS,0).edit().putString("mode",mode).commit();}
 void pump(){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1));}
 @Test public void defaultsAndInvalidPreferencesAreSafe(){assertEquals("auto",GlassOptions.mode(context()));mode("invalid");assertEquals("auto",GlassOptions.mode(context()));mode("solid");assertEquals("Solid",GlassOptions.label(context()));assertFalse(GlassWindows.eligible(context()));mode("lite");assertFalse(GlassWindows.eligible(context()));}
 Bitmap material(int kind,boolean focused){Bitmap b=Bitmap.createBitmap(300,180,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);GlassMaterial d=new GlassMaterial(context(),kind,focused,false);d.setBounds(0,0,300,180);d.draw(canvas);return b;}
 @Test public void solidFillIsOpaqueAndFocusIsDifferent(){mode("solid");Bitmap plain=material(GlassMaterial.TILE,false),focus=material(GlassMaterial.TILE,true);assertEquals(0xff172332,plain.getPixel(150,90));assertNotEquals(plain.getPixel(150,90),focus.getPixel(150,90));plain.recycle();focus.recycle();}
 @Test public void controlsAndTilesStayReadableOverWhiteBackdrop(){mode("auto");for(int kind:new int[]{GlassMaterial.CONTROL,GlassMaterial.TILE,GlassMaterial.PANEL}){Bitmap b=material(kind,false);int bg=b.getPixel(150,35);assertTrue("white text contrast",contrast(GlassUi.WHITE,bg)>=4.5);assertTrue("secondary text contrast",contrast(GlassUi.MUTED,bg)>=4.5);b.recycle();}}
 double luminance(int color){double[] a={Color.red(color)/255.0,Color.green(color)/255.0,Color.blue(color)/255.0};for(int i=0;i<3;i++)a[i]=a[i]<=.04045?a[i]/12.92:Math.pow((a[i]+.055)/1.055,2.4);return .2126*a[0]+.7152*a[1]+.0722*a[2];}
 double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 @Test public void lowRamDevicesNeverRequestSystemBlur(){ActivityManager manager=(ActivityManager)context().getSystemService(Context.ACTIVITY_SERVICE);Shadows.shadowOf(manager).setIsLowRamDevice(true);mode("auto");assertTrue(GlassOptions.lowRam(context()));assertFalse(GlassWindows.eligible(context()));}
 @Test public void materialHasRoundedOutlineAndHonorsZeroAlpha(){GlassMaterial d=new GlassMaterial(context(),GlassMaterial.DOCK,false,false);d.setBounds(0,0,300,180);Outline o=new Outline();d.getOutline(o);assertTrue(o.canClip());Bitmap b=Bitmap.createBitmap(300,180,Bitmap.Config.ARGB_8888);d.setAlpha(0);d.draw(new Canvas(b));assertEquals(0,b.getPixel(150,90));assertEquals(0,d.getAlpha());b.recycle();}
 @Test public void disabledAndFocusedControlsUseDistinctStates(){TextView b=new TextView(context());GlassUi.focus(b);b.setEnabled(false);b.refreshDrawableState();assertFalse(b.getBackground().getCurrent() instanceof GlassMaterial);b.setEnabled(true);b.requestFocus();b.refreshDrawableState();assertTrue(b.getBackground().getCurrent() instanceof GlassMaterial);}
 @Test public void appearanceMenuPersistsWithoutChangingOtherSettings(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){
  context().getSharedPreferences("solat",0).edit().putString("zone","SWK08").commit();context().getSharedPreferences(AppArtwork.PREFS,0).edit().putString("pack","test.pack").commit();int[] changes={0};GlassOptions.settings(c.get(),()->{changes[0]++;c.get().refreshGlass();});AlertDialog d=(AlertDialog)ShadowDialog.getLatestDialog();assertEquals(3,d.getListView().getCount());d.getListView().performItemClick(null,2,2);pump();assertEquals("solid",GlassOptions.mode(context()));assertEquals(1,changes[0]);assertEquals("SWK08",context().getSharedPreferences("solat",0).getString("zone",""));assertEquals("test.pack",context().getSharedPreferences(AppArtwork.PREFS,0).getString("pack",""));
 }}
 @Test @org.robolectric.annotation.Config(sdk=26) public void android8FallsBackWithoutNewApiCalls(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){assertFalse(GlassWindows.eligible(c.get()));c.get().message("Fallback","API 26 native material");Dialog d=ShadowDialog.getLatestDialog();assertTrue(d.getWindow().getDecorView().getBackground() instanceof GlassMaterial);d.dismiss();}}
 @Test public void aiSettingsKeepsSecureWindowAndGlassMaterial(){try(var c=Robolectric.buildActivity(DashboardActivity.class).setup()){c.get().aiSettings();Dialog d=ShadowDialog.getLatestDialog();assertTrue((d.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);assertTrue(d.getWindow().getDecorView().getBackground() instanceof GlassMaterial);d.dismiss();}}
 void capture(View root,Dialog dialog,int w,int h,String name)throws Exception{
  Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);root.draw(cv);
  if(dialog!=null){WindowManager.LayoutParams p=dialog.getWindow().getAttributes();View decor=dialog.getWindow().getDecorView();int dw=p.width>0?p.width:Math.min(w-48,GlassUi.dp(dialog.getContext(),560*Math.min(w/(dialog.getContext().getResources().getDisplayMetrics().density*960f),h/(dialog.getContext().getResources().getDisplayMetrics().density*540f)))),dh=p.height>0?p.height:h-64;
   decor.measure(View.MeasureSpec.makeMeasureSpec(dw,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(dh,p.height>0?View.MeasureSpec.EXACTLY:View.MeasureSpec.AT_MOST));dh=decor.getMeasuredHeight();decor.layout(0,0,dw,dh);boolean right=(p.gravity&Gravity.HORIZONTAL_GRAVITY_MASK)==Gravity.RIGHT;boolean top=(p.gravity&Gravity.VERTICAL_GRAVITY_MASK)==Gravity.TOP;cv.save();cv.translate(right?w-p.x-dw:(w-dw)/2f,top?p.y:(h-dh)/2f);decor.draw(cv);cv.restore();assertTrue(dw<=w&&dh<=h);
  }
  File file=new File(System.getProperty("user.dir"),"build/"+name+".png");file.getParentFile().mkdirs();try(var out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
 }
 void preview(int w,int h,String suffix)throws Exception{
  helper.demoApps();helper.seedApiCache();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
   DashboardActivity a=c.get();View root=helper.render(a,w,h);try(var in=a.getAssets().open("weather-wallpapers/cloudy.jpg")){((ImageView)((ViewGroup)root).getChildAt(0)).setImageBitmap(BitmapFactory.decodeStream(in));}
   TextView note=GlassUi.text(a,"UI TEST FIXTURE · sample apps · not a physical-TV capture",10,Color.WHITE);note.setBackgroundColor(0xdd101b27);FrameLayout.LayoutParams np=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);np.bottomMargin=a.d(185);((FrameLayout)root).addView(note,np);
   root=helper.render(a,w,h);View dock=root.findViewWithTag("glass-dock");assertNotNull(dock);assertFalse(dock.isFocusable());RecyclerView grid=root.findViewById(R.id.grid);assertTrue(grid.getTop()>h*.72);assertTrue(grid.getBottom()<h*.97);assertNotNull(grid.getFocusedChild());
   for(int i=0;i<grid.getChildCount();i++){View tile=grid.getChildAt(i);float top=tile.getTop()+tile.getHeight()*(1-tile.getScaleY())/2+tile.getTranslationY(),bottom=tile.getBottom()+tile.getHeight()*(tile.getScaleY()-1)/2+tile.getTranslationY();assertTrue("scaled tile must not be clipped",top>=0&&bottom<=grid.getHeight());if(i>0){View previous=grid.getChildAt(i-1);float right=previous.getRight()+previous.getWidth()*(previous.getScaleX()-1)/2;float left=tile.getLeft()+tile.getWidth()*(1-tile.getScaleX())/2;assertTrue("scaled cards need a visible gap",left-right>=1);}}
   capture(root,null,w,h,"native-glass-home-"+suffix);
   a.showJarvis();Dialog dialog=ShadowDialog.getLatestDialog();capture(root,dialog,w,h,"native-glass-jarvis-"+suffix);
   assertTrue(dialog.getWindow().getDecorView().getBackground() instanceof GlassMaterial);JarvisRepairTest.find(dialog.getWindow().getDecorView(),"Speedtest").performClick();capture(root,dialog,w,h,"native-glass-speed-"+suffix);dialog.dismiss();
   a.settings();dialog=ShadowDialog.getLatestDialog();capture(root,dialog,w,h,"native-glass-settings-"+suffix);dialog.dismiss();
   GlassOptions.settings(a,a::refreshGlass);dialog=ShadowDialog.getLatestDialog();capture(root,dialog,w,h,"native-glass-appearance-"+suffix);dialog.dismiss();
   if(suffix.equals("1080")){a.aiSettings();dialog=ShadowDialog.getLatestDialog();capture(root,dialog,w,h,"native-glass-ai-"+suffix);dialog.dismiss();}
   mode("solid");a.refreshGlass();helper.render(a,w,h);a.showJarvis();dialog=ShadowDialog.getLatestDialog();capture(root,dialog,w,h,"native-glass-solid-"+suffix);dialog.dismiss();
  }
 }
 @Test public void preview1080()throws Exception{preview(1920,1080,"1080");}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void preview720()throws Exception{preview(1280,720,"720");}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void preview4k()throws Exception{preview(3840,2160,"4k");}
}
