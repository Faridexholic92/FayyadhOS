package com.gathin.tvhome;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import java.util.*;

/** TV entries take priority; also expose sideloaded phone apps that have a launcher activity. */
final class AppCatalog {
    static List<ResolveInfo> query(Context context) {
        List<ResolveInfo> result=new ArrayList<>(); Set<String> seen=new HashSet<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            for(ResolveInfo info:context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)) {
                if(info.activityInfo==null || info.activityInfo.applicationInfo==null) continue;
                String pkg=info.activityInfo.packageName;
                if(pkg.equals(context.getPackageName()) || !seen.add(pkg)) continue;
                result.add(info);
            }
        }
        return result;
    }
}
