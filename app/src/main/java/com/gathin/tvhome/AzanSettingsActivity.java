package com.gathin.tvhome;

import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;
import java.time.*;import java.time.format.DateTimeFormatter;

/** Remote-accessible opt-in settings. Audio arrives only via the user's document picker. */
public final class AzanSettingsActivity extends Activity {
 private static final int GENERAL=9132,SUBUH=9133,NOTIFY=9134;private TextView status;private Thread importer;private boolean destroyed;private final Handler handler=new Handler(Looper.getMainLooper());
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);int pad=GlassUi.dp(this,24);box.setPadding(pad,pad,pad,pad);box.setBackgroundColor(0xff041522);
  TextView title=GlassUi.text(this,"Azan · Lima waktu",24,GlassUi.WHITE);box.addView(title);status=GlassUi.text(this,"",14,GlassUi.WHITE);status.setPadding(0,12,0,16);box.addView(status);
  add(box,"Aktif / matikan azan automatik",this::toggle);add(box,"Pilih audio azan umum (wajib)",()->pick(GENERAL));add(box,"Pilih audio Subuh (pilihan)",()->pick(SUBUH));
  add(box,"Uji bunyi audio umum",this::test);add(box,"Hentikan audio",()->{stopService(new Intent(this,AzanService.class));AzanStore.note(this,"Audio dihentikan.");update();});
  add(box,"Kebenaran alarm tepat",this::exactPermission);add(box,"Kebenaran notifikasi",this::notificationPermission);add(box,"Ke Home — OK pada waktu solat untuk jadual / zon",()->{startActivity(new Intent(this,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();});
  add(box,"Padam semua audio dan matikan azan",()->new AlertDialog.Builder(this).setTitle("Padam audio azan?").setMessage("Azan automatik dimatikan. Fail asal pada USB / storan tidak dipadam; hanya salinan aplikasi.").setPositiveButton("Padam",(d,w)->{AzanScheduler.disable(this);AzanStore.named(this,false).delete();AzanStore.named(this,true).delete();update();}).setNegativeButton("Batal",null).show());
  add(box,"Tutup",this::finish);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(box);setContentView(scroll);update();
 }
 private void add(LinearLayout box,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setFocusable(true);b.setOnClickListener(v->action.run());box.addView(b,new LinearLayout.LayoutParams(-1,GlassUi.dp(this,48)));}
 private void message(String text){new AlertDialog.Builder(this).setTitle("Azan").setMessage(text).setPositiveButton("OK",null).show();}
 private void update(){if(status==null||destroyed)return;
  var prefs=AzanStore.prefs(this);long at=prefs.getLong("pending_at",0);String next=at<=0?"Belum dijadualkan":Instant.ofEpochMilli(at).atZone(PrayerTimes.MALAYSIA).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm",java.util.Locale.ENGLISH))+" MYT";
  status.setText("Automatik: "+(AzanStore.enabled(this)?"ON":"OFF")+" · Zon: "+(AzanScheduler.zone(this).isEmpty()?"Belum dipilih":AzanScheduler.zone(this))+"\nAudio umum: "+(AzanStore.present(AzanStore.named(this,false))?"Tersimpan — uji bunyi dahulu":"Belum dipilih")+"\nAudio Subuh: "+(AzanStore.present(AzanStore.named(this,true))?"Audio berasingan":"Guna audio umum")+"\nSeterusnya: "+next+"\n"+prefs.getString("status","")+"\n\nTiada audio azan dibekalkan. Pilih rakaman yang anda berhak guna. Azan dimainkan mengikut jadual JAKIM tersimpan, termasuk ketika Home tidak terbuka jika Android membenarkan. Tiada arahan pause atau permintaan audio focus dihantar kepada aplikasi lain; audio boleh bertindih. Guna volume media TV sedia ada. Firmware mungkin berbeza. Tiada jaminan ketika TV dimatikan sepenuhnya / aplikasi force-stop.");
 }
 private void toggle(){if(AzanStore.enabled(this)){AzanScheduler.disable(this);update();return;}String problem=AzanScheduler.problem(this);if(!problem.isEmpty()){message(problem);return;}
  new AlertDialog.Builder(this).setTitle("Aktifkan azan lima waktu?").setMessage("Audio anda akan dimainkan secara automatik pada Subuh, Zohor, Asar, Maghrib dan Isyak. Tiada Syuruk. Boleh bertindih dengan movie kerana aplikasi ini tidak meminta aplikasi lain pause. Jadual dikemas kini sekali sehari melalui tugas Android bila rangkaian tersedia. Aktifkan hanya selepas uji bunyi.").setPositiveButton("Aktifkan",(d,w)->{AzanStore.prefs(this).edit().putBoolean("enabled",true).commit();AzanStore.note(this,"Azan automatik diaktifkan oleh pengguna.");AzanScheduler.reconcile(this);update();}).setNegativeButton("Batal",null).show();
 }
 private void pick(int request){if(importer!=null&&importer.isAlive()){message("Tunggu import audio selesai.");return;}try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),request);}catch(ActivityNotFoundException e){message("TV ini tiada pemilih fail audio. Pasang / gunakan pemilih dokumen yang dipercayai, atau sediakan fail audio untuk dimasukkan ke projek. Jangan masukkan kata laluan atau kunci API.");}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if((request!=GENERAL&&request!=SUBUH)||result!=RESULT_OK||data==null||data.getData()==null)return;
  Uri uri=data.getData();status.setText("Mengimport audio ke storan dalaman aplikasi…");
  importer=new Thread(()->{String error=null;try{AzanStore.importAudio(getApplicationContext(),uri,request==SUBUH);AzanStore.note(this,"Audio disimpan. Uji bunyi sebelum aktifkan automatik.");}catch(Exception e){error=e instanceof java.io.IOException?e.getMessage():"Fail tidak boleh dibaca. Pilih semula audio.";}
   final String resultText=error;handler.post(()->{if(destroyed)return;if(resultText!=null)message(resultText);else AzanScheduler.reconcile(this);update();});
  },"azan-audio-import");importer.start();
 }
 private void test(){if(!AzanStore.present(AzanStore.named(this,false))){message("Pilih audio azan umum dahulu.");return;}if(!AzanScheduler.notificationsAllowed(this)){message("Benarkan notifikasi sebelum uji audio.");return;}
  new AlertDialog.Builder(this).setTitle("Mainkan ujian azan sekarang?").setMessage("Bunyi akan kedengaran menggunakan volume media TV. Hentikan melalui butang Hentikan audio atau notifikasi.").setPositiveButton("Mainkan",(d,w)->{try{startForegroundService(new Intent(this,AzanService.class).setAction(AzanService.TEST).putExtra("index",2));}catch(RuntimeException e){AzanStore.note(this,"Android menyekat main balik audio.");}update();}).setNegativeButton("Batal",null).show();
 }
 private void exactPermission(){if(Build.VERSION.SDK_INT<31){message("Android ini tidak memerlukan kebenaran alarm tepat berasingan.");return;}try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){message("Firmware TV ini tidak menyediakan skrin kebenaran alarm tepat. Automatik tidak diaktifkan tanpa kebenaran yang diperlukan.");}}
 private void notificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFY);return;}try{startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}catch(ActivityNotFoundException e){message("Skrin notifikasi tidak tersedia pada firmware ini.");}}
 @Override protected void onResume(){super.onResume();AzanScheduler.reconcile(this);update();}
 @Override public void onRequestPermissionsResult(int request,String[] p,int[] r){super.onRequestPermissionsResult(request,p,r);if(request==NOTIFY){AzanScheduler.reconcile(this);update();}}
 @Override protected void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);if(importer!=null)importer.interrupt();super.onDestroy();}
}
