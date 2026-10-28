package com.gathin.tvhome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.StatFs;
import android.text.format.Formatter;

/** Ordinary app APIs only; no privileged probing, serial collection or guessed hardware specs. */
final class SystemInfo {
    static String report(Context c){
        StringBuilder text=new StringBuilder("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT)
            .append(")\nManufacturer: ").append(Build.MANUFACTURER).append("\nModel: ").append(Build.MODEL);
        try{PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);text.append("\nApp: ").append(p.versionName).append(" (").append(Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode).append(")");}catch(Exception e){text.append("\nApp version: unavailable");}
        try{ActivityManager manager=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);if(manager==null)throw new IllegalStateException();ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();manager.getMemoryInfo(m);
            text.append("\n\nRAM available now: ").append(Formatter.formatFileSize(c,m.availMem)).append("\nRAM reported by Android: ").append(Formatter.formatFileSize(c,m.totalMem)).append("\nLow-memory status: ").append(m.lowMemory?"Yes":"No");}catch(Exception e){text.append("\nRAM: unavailable");}
        try{StatFs disk=new StatFs(c.getFilesDir().getPath());text.append("\n\nApp-data filesystem available: ").append(Formatter.formatFileSize(c,disk.getAvailableBytes())).append("\nApp-data filesystem total: ").append(Formatter.formatFileSize(c,disk.getTotalBytes()));}catch(Exception e){text.append("\nStorage: unavailable");}
        NetworkSnapshot network=NetworkSnapshot.read(c);text.append("\n\nNetwork: ").append(network.transport).append("\nAndroid Internet validation: ").append(network.validated?"Validated":"Not validated");
        return text.append("\n\nLive Android readings at panel opening. Available RAM varies; filesystem totals are not advertised storage capacity. Upload, latency and packet loss are not measured. No root or additional permission is used.").toString();
    }
}
