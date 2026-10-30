package com.gathin.tvhome;

import android.content.Context;
import java.io.*;
import java.util.Arrays;

/** Fixed offline collection, no network, external storage or arbitrary paths. */
final class FayyadhWallpapers {
    static final String DEFAULT="10-Cosmic-Sunrise.jpg";
    static final String[] FILES={"01-Tropical-Morning.jpg","02-Candy-Clouds.jpg","03-Sunflower-Hills.jpg",
            "04-Coral-Reef.jpg","05-Celestial-Garden.jpg","06-Borneo-Morning.jpg","07-Citrus-Coast.jpg",
            "08-Blossom-Lake.jpg","09-Festival-Sky.jpg","10-Cosmic-Sunrise.jpg",DEFAULT};
    static final String[] NAMES={"01 · Tropical Morning","02 · Candy Clouds","03 · Sunflower Hills",
            "04 · Coral Reef","05 · Celestial Garden","06 · Borneo Morning","07 · Citrus Coast",
            "08 · Blossom Lake","09 · Festival Sky","10 · Cosmic Sunrise","11 · Kinabalu Sunset"};
    static boolean contains(String name) { return Arrays.asList(FILES).contains(name); }
    static File file(Context context,String name) {
        if(!contains(name)) throw new IllegalArgumentException("Unknown wallpaper");
        File directory=new File(context.getFilesDir(),"fayyadh-wallpapers");
        File result=new File(directory,name);
        if(result.isFile() && result.length()>0) return result;
        if(!directory.isDirectory() && !directory.mkdirs()) return result;
        File temporary=new File(directory,name+".tmp");
        try(InputStream in=context.getAssets().open("fayyadh-wallpapers/"+name);
            OutputStream out=new FileOutputStream(temporary)) {
            byte[] buffer=new byte[16384]; int n;
            while((n=in.read(buffer))!=-1) out.write(buffer,0,n);
            out.flush();
        } catch(IOException failure) { temporary.delete(); return result; }
        if(!temporary.renameTo(result)) temporary.delete();
        return result;
    }
}
