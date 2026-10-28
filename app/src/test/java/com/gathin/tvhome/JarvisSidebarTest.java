package com.gathin.tvhome;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class JarvisSidebarTest {
 final DashboardTest helper=new DashboardTest();
 @Before public void silent(){RuntimeEnvironment.getApplication().getSharedPreferences("jarvis",0).edit().putBoolean("voice",false).commit();}
 void layout(int screenW,int screenH,String name)throws Exception{helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  View root=helper.render(c.get(),screenW,screenH);View grid=root.findViewById(R.id.grid);int before=grid.getWidth();c.get().showJarvis();Dialog d=ShadowDialog.getLatestDialog();WindowManager.LayoutParams p=d.getWindow().getAttributes();
  assertEquals(Gravity.RIGHT|Gravity.TOP,p.gravity);assertTrue(p.width<=screenW/3);assertTrue(p.height<=screenH);assertTrue(p.y+p.height<=screenH);assertEquals(before,grid.getWidth());
  View decor=d.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(p.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(p.height,View.MeasureSpec.EXACTLY));decor.layout(0,0,p.width,p.height);
  for(String label:new String[]{"Cuaca Kinarut","Speedtest","Buka YouTube","Maklumat Sistem","Tetapan Wi-Fi","Aplikasi","Jarvis AI Settings","Explain with AI","Options","Close"}){TextView b=JarvisRepairTest.find(decor,label);assertNotNull(label,b);assertTrue(label,b.isFocusable());assertTrue(label,b.isClickable());}
  ScrollView scroll=(ScrollView)JarvisRepairTest.field(JarvisRepairTest.field(c.get(),"jarvis"),"scroll");View box=scroll.getChildAt(0);
  Bitmap pixels=Bitmap.createBitmap(p.width,p.height,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(pixels));assertEquals("Opaque panel interior",255,Color.alpha(pixels.getPixel(p.width/2,p.height/2)));pixels.recycle();
  Bitmap composite=Bitmap.createBitmap(screenW,screenH,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(composite);root.draw(canvas);canvas.save();canvas.translate(screenW-p.x-p.width,p.y);decor.draw(canvas);canvas.restore();try(var out=new java.io.FileOutputStream(new java.io.File(System.getProperty("user.dir"),"build/"+name+"-FIXTURE.png"))){composite.compress(Bitmap.CompressFormat.PNG,100,out);}composite.recycle();
  JarvisRepairTest.find(decor,"Close").performClick();JarvisRepairTest.pump();helper.render(c.get(),screenW,screenH);assertEquals(before,grid.getWidth());assertFalse(d.isShowing());
 }}
 @Test public void sidebar1080UsesRightWindowAndOpaqueInterior()throws Exception{layout(1920,1080,"sidebar-1080");}
 @Test @org.robolectric.annotation.Config(qualifiers="w640dp-h360dp-land-xhdpi") public void sidebar720FitsWithoutReservingHomeSpace()throws Exception{layout(1280,720,"sidebar-720");}
 @Test @org.robolectric.annotation.Config(qualifiers="w1920dp-h1080dp-land-xhdpi") public void sidebar4kFitsWithoutReservingHomeSpace()throws Exception{layout(3840,2160,"sidebar-4k");}
 @Test public void systemAndApplicationActionsStayNative()throws Exception{helper.demoApps();try(var c=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
  helper.render(c.get(),1920,1080);c.get().showJarvis();Dialog d=ShadowDialog.getLatestDialog();JarvisRepairTest.find(d.getWindow().getDecorView(),"Maklumat Sistem").performClick();AlertDialog report=(AlertDialog)ShadowDialog.getLatestDialog();String text=Shadows.shadowOf(report).getMessage().toString();assertTrue(text.contains("Android:"));assertTrue(text.contains("App-data filesystem"));assertFalse(text.contains("GroqCloud AI"));report.dismiss();JarvisRepairTest.find(d.getWindow().getDecorView(),"Aplikasi").performClick();AlertDialog apps=(AlertDialog)ShadowDialog.getLatestDialog();assertEquals(new DashboardApps(c.get()).visible().size(),apps.getListView().getAdapter().getCount());apps.dismiss();d.dismiss();
 }}
}
