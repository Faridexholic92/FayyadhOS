package com.gathin.tvhome;
import android.content.*;import java.time.Instant;
/** Non-exported, explicit alarm target. Audio only starts after an on-time, validated claim. */
public final class AzanReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent intent){
  if(intent==null||!AzanScheduler.ACTION.equals(intent.getAction()))return;
  if(AzanScheduler.take(c,intent,Instant.now())){
   try{c.startForegroundService(new Intent(c,AzanService.class).setAction(AzanService.PLAY).putExtra("index",intent.getIntExtra("index",-1)).putExtra("zone",intent.getStringExtra("zone")));}
   catch(RuntimeException denied){AzanStore.note(c,"Android menyekat audio latar. Tiada azan lewat dimainkan semula.");}
  }AzanScheduler.reconcile(c);
 }
}
