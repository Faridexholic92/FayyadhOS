package com.gathin.tvhome;

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import org.xmlpull.v1.*;
import java.io.*;
import java.util.*;

/** Local-only artwork: installed icon pack > activity TV banner > app banner > icon. */
final class AppArtwork {
    static final String PREFS="app-artwork";
    private final Context context;
    private final Map<String,String> mapping=new HashMap<>();
    private final LruCache<String,Drawable.ConstantState> cache=new LruCache<>(24);
    private Resources packResources;private String pack="";
    AppArtwork(Context context){this.context=context;refresh();}
    static String component(String value){
        if(value==null)return "";String s=value.trim();
        if(s.startsWith("ComponentInfo{")&&s.endsWith("}"))s=s.substring(14,s.length()-1);
        ComponentName name=ComponentName.unflattenFromString(s);return name==null?"":name.flattenToString();
    }
    static Map<String,String> parse(XmlPullParser parser)throws Exception{
        Map<String,String> result=new HashMap<>();int entries=0;
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
            if(++entries>40000)throw new IOException("Icon mapping too large");
            if(event!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;
            String key=component(parser.getAttributeValue(null,"component")),drawable=parser.getAttributeValue(null,"drawable");
            if(!key.isEmpty()&&drawable!=null&&drawable.matches("[a-zA-Z0-9_]+"))result.put(key,drawable);
        }
        return result;
    }
    List<ResolveInfo> packs(){
        Map<String,ResolveInfo> unique=new TreeMap<>();PackageManager pm=context.getPackageManager();
        List<Intent> queries=Arrays.asList(new Intent("org.adw.launcher.THEMES"),new Intent("com.novalauncher.THEME"),new Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME"),new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
        for(Intent intent:queries)for(ResolveInfo r:pm.queryIntentActivities(intent,0))try{
            String pkg=r.activityInfo.packageName;
            if(unique.containsKey(pkg))continue;
            if(intent.hasCategory(Intent.CATEGORY_LAUNCHER)){String name=r.loadLabel(pm).toString().toLowerCase(Locale.ROOT);if(!name.contains("icon")&&!name.contains("projectivy"))continue;}
            Resources res=pm.getResourcesForApplication(pkg);
            if(res.getIdentifier("appfilter","xml",pkg)!=0||Arrays.asList(res.getAssets().list("")).contains("appfilter.xml"))unique.put(pkg,r);
        }catch(Exception ignored){}
        return new ArrayList<>(unique.values());
    }
    void refresh(){
        cache.evictAll();mapping.clear();packResources=null;pack="";
        String selected=context.getSharedPreferences(PREFS,0).getString("pack","auto");
        if("auto".equals(selected)){
            List<ResolveInfo> found=packs();
            // Prefer the Projectivy community pack when installed; otherwise the first installed pack.
            for(ResolveInfo r:found)if(r.loadLabel(context.getPackageManager()).toString().toLowerCase(Locale.ROOT).contains("projectivy")){selected=r.activityInfo.packageName;break;}
            if("auto".equals(selected))selected=found.isEmpty()?"":found.get(0).activityInfo.packageName;
        }
        if(selected.isEmpty())return;
        try{
            Resources resources=context.getPackageManager().getResourcesForApplication(selected);Map<String,String> loaded;
            int xml=resources.getIdentifier("appfilter","xml",selected);
            if(xml!=0){try(android.content.res.XmlResourceParser parser=resources.getXml(xml)){loaded=parse(parser);}}
            else try(InputStream in=resources.getAssets().open("appfilter.xml")){
                XmlPullParser parser=XmlPullParserFactory.newInstance().newPullParser();parser.setInput(in,"UTF-8");loaded=parse(parser);
            }
            pack=selected;packResources=resources;mapping.putAll(loaded);
        }catch(Exception ignored){/* Missing/uninstalled/broken pack: native banners stay usable. */}
    }
    Drawable load(ResolveInfo app){
        String key=DashboardApps.key(app);Drawable.ConstantState state=cache.get(key);if(state!=null)return state.newDrawable(context.getResources());
        Drawable image=null;
        try{String name=mapping.get(new ComponentName(app.activityInfo.packageName,app.activityInfo.name).flattenToString());
            if(name!=null&&packResources!=null){int id=packResources.getIdentifier(name,"drawable",pack);if(id!=0)image=packResources.getDrawable(id,null);}
        }catch(Exception ignored){}
        try{if(image==null)image=app.activityInfo.loadBanner(context.getPackageManager());}catch(Exception ignored){}
        try{if(image==null)image=app.activityInfo.applicationInfo.loadBanner(context.getPackageManager());}catch(Exception ignored){}
        try{if(image==null)image=app.loadIcon(context.getPackageManager());}catch(Exception ignored){}
        if(image==null)image=context.getPackageManager().getDefaultActivityIcon();
        if(image.getConstantState()!=null)cache.put(key,image.getConstantState());return image;
    }
    void settings(DashboardActivity activity,Runnable changed){
        List<ResolveInfo> found=packs();String[] labels=new String[found.size()+2],values=new String[labels.length];
        labels[0]="Auto · installed icon pack / TV banners";values[0]="auto";labels[1]="Original TV banners / app icons";values[1]="";
        for(int i=0;i<found.size();i++){labels[i+2]=found.get(i).loadLabel(context.getPackageManager()).toString();values[i+2]=found.get(i).activityInfo.packageName;}
        String selected=context.getSharedPreferences(PREFS,0).getString("pack","auto");int index=Arrays.asList(values).indexOf(selected);
        activity.show(new AlertDialog.Builder(activity).setTitle("Full artwork · Icon pack")
            .setSingleChoiceItems(labels,index,(d,i)->{context.getSharedPreferences(PREFS,0).edit().putString("pack",values[i]).apply();refresh();changed.run();d.dismiss();})
            .setNeutralButton("Get Projectivy pack",(d,i)->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/SicMundus86/ProjectivyIconPack/releases")));}catch(Exception e){activity.message("Icon pack","Install the optional Projectivy Icon Pack from github.com/SicMundus86/ProjectivyIconPack/releases, then return and select it here. No APK is installed automatically.");}})
            .setNegativeButton("Close",null));
    }
}
