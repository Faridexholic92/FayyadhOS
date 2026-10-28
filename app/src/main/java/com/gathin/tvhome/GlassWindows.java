package com.gathin.tvhome;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.*;
import java.util.function.Consumer;

/** System-composited blur for floating windows only; a dense readable fallback is always available. */
final class GlassWindows {
    static void apply(Dialog dialog){
        Window window=dialog.getWindow();if(window==null)return;Context c=dialog.getContext();
        window.setBackgroundDrawable(new GlassMaterial(c,GlassMaterial.PANEL,false,false));
        if(Build.VERSION.SDK_INT>=31)Api31.install(dialog);
    }
    static boolean eligible(Context c){return Build.VERSION.SDK_INT>=31&&GlassOptions.mode(c).equals("auto")&&!GlassOptions.lowRam(c);}
    @android.annotation.TargetApi(31)
    private static final class Api31 {
        static void install(Dialog dialog){
            Window window=dialog.getWindow();Context c=dialog.getContext();View decor=window.getDecorView();
            WindowManager manager=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);if(manager==null)return;
            Consumer<Boolean> update=enabled->{
                boolean use=enabled&&eligible(c);
                try{window.setBackgroundBlurRadius(use?Math.min(48,GlassUi.dp(c,20)):0);window.setBackgroundDrawable(new GlassMaterial(c,GlassMaterial.PANEL,false,use));}
                catch(RuntimeException unsupported){window.setBackgroundDrawable(new GlassMaterial(c,GlassMaterial.PANEL,false,false));}
            };
            try{
                update.accept(manager.isCrossWindowBlurEnabled());
                if(!eligible(c))return;
                manager.addCrossWindowBlurEnabledListener(update);
                decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                    public void onViewAttachedToWindow(View v){}
                    public void onViewDetachedFromWindow(View v){try{manager.removeCrossWindowBlurEnabledListener(update);}catch(RuntimeException ignored){}v.removeOnAttachStateChangeListener(this);}
                });
            }catch(RuntimeException unsupported){window.setBackgroundDrawable(new GlassMaterial(c,GlassMaterial.PANEL,false,false));}
        }
    }
}
