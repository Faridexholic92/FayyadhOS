package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.util.Base64InputStream;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Bounded, verified pre-rendered 3D atlas loader. Called only on the decoration's worker. */
final class MechaFrames {
 static final int TILE_W=112,TILE_H=128;
 static final long MAX_BYTES=14L*1024*1024;
 private static final int MAX_ENCODED=768*1024;
 private final Bitmap[] images=new Bitmap[3];
 private final int sample;
 private MechaFrames(int sample){this.sample=sample;}
 static MechaFrames load(Context context,boolean lowRam)throws Exception{
  MechaFrames frames=new MechaFrames(lowRam?2:1);
  try{
   JSONObject manifest;
   try(InputStream in=context.getAssets().open("mini-mecha3d/manifest.json")){manifest=new JSONObject(new String(readLimited(in,16384),StandardCharsets.UTF_8));}
   validateManifest(manifest);JSONArray atlases=manifest.getJSONArray("atlases");long allocated=0;
   for(int phase=0;phase<3;phase++){
    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Decoration load cancelled");
    JSONObject spec=atlases.getJSONObject(phase);String name=spec.getString("name");int parts=spec.getInt("parts");ArrayList<InputStream> streams=new ArrayList<>();byte[] data;
    try{
     for(int i=0;i<parts;i++)streams.add(context.getAssets().open(String.format(Locale.ROOT,"mini-mecha3d/%s-%02d.b64",name,i)));
     try(InputStream in=new Base64InputStream(new SequenceInputStream(Collections.enumeration(streams)),android.util.Base64.DEFAULT)){data=readLimited(in,MAX_ENCODED);}
    }finally{for(InputStream in:streams)try{in.close();}catch(IOException ignored){}}
    verifyBytes(data,spec.getInt("bytes"),spec.getString("sha256"));
    BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,bounds);
    if(bounds.outWidth!=spec.getInt("width")||bounds.outHeight!=spec.getInt("height"))throw new IOException("Invalid 3D atlas bounds");
    BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=frames.sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);frames.images[phase]=bitmap;
    if(bitmap==null||bitmap.getWidth()!=bounds.outWidth/frames.sample||bitmap.getHeight()!=bounds.outHeight/frames.sample)throw new IOException("3D atlas decode failed");
    allocated+=bitmap.getAllocationByteCount();if(allocated>MAX_BYTES/(frames.sample*frames.sample))throw new IOException("3D atlas memory limit exceeded");
   }
   return frames;
  }catch(Exception|OutOfMemoryError failure){frames.recycle();throw failure;}
 }
 static void validateManifest(JSONObject manifest)throws Exception{
  if(manifest.getInt("schema")!=1||manifest.getInt("tileWidth")!=TILE_W||manifest.getInt("tileHeight")!=TILE_H)throw new IOException("Invalid 3D atlas manifest");
  JSONArray rows=manifest.getJSONArray("atlases");if(rows.length()!=3)throw new IOException("Invalid atlas count");
  String[] names={"run","fire","recover"};int[] counts={32,64,32},columns={8,16,8};
  for(int i=0;i<3;i++){JSONObject r=rows.getJSONObject(i);int parts=r.getInt("parts"),bytes=r.getInt("bytes");
   if(!names[i].equals(r.getString("name"))||r.getInt("frames")!=counts[i]||r.getInt("columns")!=columns[i]||r.getInt("width")!=TILE_W*columns[i]||r.getInt("height")!=1024||parts<1||parts>32||bytes<1||bytes>MAX_ENCODED||!r.getString("sha256").matches("[0-9a-f]{64}"))throw new IOException("Invalid atlas specification");
  }
 }
 static byte[] readLimited(InputStream input,int limit)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>limit)throw new IOException("Atlas input too large");bytes.write(buffer,0,count);}return bytes.toByteArray();}
 static void verifyBytes(byte[] data,int expected,String digest)throws Exception{
  byte[] hash=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder actual=new StringBuilder(64);for(byte value:hash)actual.append(String.format(Locale.ROOT,"%02x",value&255));
  if(data.length!=expected||!actual.toString().equals(digest))throw new IOException("3D atlas integrity check failed");
 }
 long allocatedBytes(){long bytes=0;for(Bitmap image:images)if(image!=null&&!image.isRecycled())bytes+=image.getAllocationByteCount();return bytes;}
 int sample(){return sample;}
 void draw(Canvas canvas,int phase,int row,int frame,Rect source,RectF target,Paint paint){int columns=phase==1?16:8,count=phase==1?64:32;int index=row*count+Math.max(0,Math.min(count-1,frame));int w=TILE_W/sample,h=TILE_H/sample;source.set(index%columns*w,index/columns*h,(index%columns+1)*w,(index/columns+1)*h);canvas.drawBitmap(images[phase],source,target,paint);}
 void recycle(){for(int i=0;i<images.length;i++)if(images[i]!=null){images[i].recycle();images[i]=null;}}
}
