package com.gathin.tvhome;
import android.app.AlertDialog;
import android.content.*;
import android.net.*;
import android.net.wifi.*;
import android.provider.Settings;
import android.widget.Toast;
import java.util.*;

final class NetworkTools {
    static void show(MainActivity activity) {
        boolean on=NetworkFocus.enabled(activity);
        new AlertDialog.Builder(activity).setTitle("Rangkaian · "+(on?"Mod fokus aktif":"Normal"))
            .setItems(new String[]{"Semak sambungan / link","Boost ringan: kurangkan trafik launcher",
                    "Restore: pulihkan mod sebelum boost","Tetapan Wi-Fi / pilih 5 GHz","Tetapan rangkaian / LAN","Panduan Wi-Fi / LAN"},(d,i)->{
                if(i==0) message(activity,"Status rangkaian",report(activity));
                else if(i==1) new AlertDialog.Builder(activity).setTitle("Aktifkan mod fokus rangkaian?")
                    .setMessage("Hanya hentikan kemas kini AUTOMATIK jadual JAKIM dan gambar Places dalam launcher ini. Waktu solat cache dan wallpaper pilihan kekal. Muat semula JAKIM manual masih boleh. Permintaan yang telah bermula mungkin selesai.\n\nIni bukan peningkatan bandwidth atau kelajuan port. Tiada perubahan DNS, router, VPN atau app lain. Tetapan asal dua ciri ini disimpan untuk Restore.")
                    .setPositiveButton("Aktifkan",(x,w)->{
                        boolean ok=NetworkFocus.enable(activity);
                        Toast.makeText(activity,ok?"Mod fokus aktif · bukan jaminan speed bertambah":"Tetapan tidak dapat disimpan",Toast.LENGTH_LONG).show();
                        activity.onNetworkFocusChanged();
                    }).setNegativeButton("Batal",null).show();
                else if(i==2) {
                    if(!on) {message(activity,"Restore","Tiada snapshot boost untuk dipulihkan. Tiada tetapan diubah.");return;}
                    new AlertDialog.Builder(activity).setTitle("Restore mod sebelum boost?")
                        .setMessage("Pulihkan pilihan kemas kini automatik JAKIM dan Places daripada snapshot launcher. Ini TIDAK memulihkan tetapan Wi-Fi/LAN yang anda ubah sendiri dalam tetapan Android.")
                        .setPositiveButton("Restore",(x,w)->{
                            boolean ok=NetworkFocus.restore(activity);
                            Toast.makeText(activity,ok?"Pilihan asal launcher dipulihkan":"Snapshot tidak lengkap; tiada tetapan dipulihkan",Toast.LENGTH_LONG).show();
                            activity.onNetworkFocusChanged();
                        }).setNegativeButton("Batal",null).show();
                } else if(i==3) activity.openNetworkSettings(Settings.ACTION_WIFI_SETTINGS);
                else if(i==4) activity.openNetworkSettings(Settings.ACTION_WIRELESS_SETTINGS);
                else message(activity,"Wi-Fi / LAN · cari bottleneck",
                    "• Port LAN 100 Mbps tidak boleh mencapai 500 Mbps. Semak spesifikasi model box dan link pada router.\n\n• Untuk melebihi 100 Mbps, laluan LAN perlu Gigabit: box/adapter, kabel dan router. USB Gigabit hanya jika serasi dengan box.\n\n• Cuba Wi-Fi 5 GHz dekat router jika disokong; banyak dinding boleh menjadikan 2.4 GHz lebih sesuai. Link rate bukan kelajuan Internet.\n\n• Bandingkan speedtest box dan telefon/laptop pada lokasi, pelayan dan masa yang sama. Tiada ujian kelajuan atau muat turun besar dimulakan di sini.\n\n• Jangan matikan firewall, root tweak atau guna speed booster tidak dikenali. DNS tidak mengubah had bandwidth port.\n\nSumber: TP-Link FAQ 2435\nhttps://www.tp-link.com/us/support/faq/2435");
            }).setNegativeButton("Tutup",null).show();
    }
    static void message(Context c,String title,String body) {new AlertDialog.Builder(c).setTitle(title).setMessage(body).setPositiveButton("Tutup",null).show();}
    static String report(Context context) {
        try {
            ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active=cm==null?null:cm.getActiveNetwork();
            NetworkCapabilities caps=active==null?null:cm.getNetworkCapabilities(active);
            if(caps==null) return "Tiada rangkaian aktif yang dapat dibaca. Semak Wi-Fi/LAN.\n\nIni bukan ujian kelajuan Internet.";
            List<String> transport=new ArrayList<>();
            if(caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transport.add("LAN / Ethernet");
            if(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transport.add("Wi-Fi");
            if(caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) transport.add("VPN (rangkaian asas mungkin berbeza)");
            String report="Sambungan aktif: "+(transport.isEmpty()?"Jenis lain":android.text.TextUtils.join(", ",transport))+"\nInternet disahkan Android: "+(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Ya":"Belum / tidak tersedia");
            if(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiManager wm=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wi=wm==null?null:wm.getConnectionInfo();
                if(wi!=null) {
                    if(wi.getFrequency()>0) report+="\nFrekuensi: "+wi.getFrequency()+" MHz";
                    if(wi.getLinkSpeed()>0) report+="\nLink Wi-Fi: "+wi.getLinkSpeed()+" Mbps (BUKAN speedtest)";
                }
            }
            return report+"\n\nKelajuan Internet dan had port LAN tidak diukur oleh paparan ini. Kadar pelan bukan jaminan kadar box. Tiada SSID, IP atau data rangkaian dihantar keluar.";
        } catch(RuntimeException failure) {
            return "Maklumat sambungan disekat atau tidak tersedia pada firmware ini. Tiada nilai kelajuan diteka. Semak melalui tetapan Android. Ini bukan speedtest.";
        }
    }
}
