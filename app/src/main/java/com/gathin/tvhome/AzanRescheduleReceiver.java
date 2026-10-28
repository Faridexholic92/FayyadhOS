package com.gathin.tvhome;
import android.content.*;
/** Rebuild future alarms only; boot/clock changes NEVER directly play audio. */
public final class AzanRescheduleReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(i==null)return;String a=i.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)||android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(a)){AzanScheduler.cancelAlarm(c);AzanScheduler.reconcile(c);}}
}
