package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** Explicit user-started special-use FGS: 35% dim across apps, no input interception. */
public final class DimService extends Service {
    static final String PREFS="screen-dim",STOP="com.gathin.tvhome.DIM_STOP";
    static final int ID=3501,COLOR=0x59000000;
    private WindowManager windows;private View shade;
    private AppOpsManager appOps;
    private final AppOpsManager.OnOpChangedListener permissions=(op,pkg)->{if(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW.equals(op)&&getPackageName().equals(pkg)&&!Settings.canDrawOverlays(this))new Handler(Looper.getMainLooper()).post(this::off);};
    @Override public void onCreate(){super.onCreate();appOps=getSystemService(AppOpsManager.class);if(appOps!=null)appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,getPackageName(),permissions);}
    static boolean enabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean("enabled",false);}
    static void disable(Context c){c.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",false).commit();c.stopService(new Intent(c,DimService.class));}
    static boolean enable(Context c){
        if(!Settings.canDrawOverlays(c))return false;
        c.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",true).commit();
        try{Intent intent=new Intent(c,DimService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(intent);else c.startService(intent);return true;}
        catch(RuntimeException denied){c.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",false).commit();return false;}
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&STOP.equals(intent.getAction())||!enabled(this)||!Settings.canDrawOverlays(this)){off();return START_NOT_STICKY;}
        try{
            NotificationManager manager=getSystemService(NotificationManager.class);
            NotificationChannel channel=new NotificationChannel("screen-dim","Screen dim",NotificationManager.IMPORTANCE_LOW);channel.setSound(null,null);manager.createNotificationChannel(channel);
            PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,DimService.class).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent home=PendingIntent.getActivity(this,1,new Intent(this,DashboardActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notification=new Notification.Builder(this,"screen-dim").setSmallIcon(R.drawable.ic_settings).setContentTitle("FayyadhOS · Dim 35% ON").setContentText("Open Settings or tap OFF to restore the screen.").setOngoing(true).setContentIntent(home).addAction(new Notification.Action.Builder(null,"OFF",stop).build()).build();
            if(Build.VERSION.SDK_INT>=34)startForeground(ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(ID,notification);
            if(shade==null){windows=(WindowManager)getSystemService(WINDOW_SERVICE);shade=new View(this);shade.setBackgroundColor(COLOR);shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
                p.setTitle("FayyadhOS 35% dim");windows.addView(shade,p);}
            return START_STICKY;
        }catch(RuntimeException denied){off();return START_NOT_STICKY;}
    }
    private void off(){getSharedPreferences(PREFS,0).edit().putBoolean("enabled",false).commit();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){if(appOps!=null)appOps.stopWatchingMode(permissions);if(shade!=null&&windows!=null){try{windows.removeViewImmediate(shade);}catch(RuntimeException ignored){}shade=null;}super.onDestroy();}
}
