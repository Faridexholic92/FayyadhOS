package com.gathin.tvhome;

import android.content.*;import android.net.Uri;
import java.io.*;

/** User-selected, local audio only. Never embeds/downloads a recording or substitutes TTS. */
final class AzanStore {
 static final long MAX_BYTES=25L*1024*1024;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("azan-v1",0);}
 static boolean enabled(Context c){return prefs(c).getBoolean("enabled",false);}
 static File named(Context c,boolean subuh){return new File(c.getFilesDir(),subuh?"azan-subuh.audio":"azan-main.audio");}
 static boolean present(File f){return f.isFile()&&f.length()>0;}
 static File audio(Context c,int index){File special=named(c,true);return index==0&&present(special)?special:named(c,false);}
 static void note(Context c,String text){prefs(c).edit().putString("status",text).apply();}
 static synchronized void importAudio(Context c,Uri uri,boolean subuh)throws IOException{
  if(uri==null||!"content".equals(uri.getScheme()))throw new IOException("Pilih audio melalui pemilih fail Android.");
  String mime=c.getContentResolver().getType(uri);if(mime!=null&&!mime.startsWith("audio/")&&!mime.equals("application/octet-stream"))throw new IOException("Fail yang dipilih bukan audio.");
  File file=named(c,subuh),temp=new File(file.getPath()+".tmp");boolean saved=false;
  try{
   InputStream raw=c.getContentResolver().openInputStream(uri);if(raw==null)throw new IOException("Fail tidak boleh dibaca.");
   try(InputStream in=raw;FileOutputStream out=new FileOutputStream(temp)){
    byte[] buffer=new byte[16384];long count=0;int n;
    while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Import dibatalkan.");count+=n;if(count>MAX_BYTES)throw new IOException("Had audio ialah 25 MiB.");out.write(buffer,0,n);}
    if(count==0)throw new IOException("Fail audio kosong.");out.getFD().sync();
   }
   if(!temp.renameTo(file))throw new IOException("Audio tidak dapat disimpan.");saved=true;
  }finally{if(!saved)temp.delete();}
 }
}
