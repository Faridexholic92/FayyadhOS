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

/** Installed pack > TV banners > icon. Direct known-pack visibility, alias/package fallback, diagnostics. */
final class AppArtwork {
    static final String PREFS="app-artwork",PROJECTIVY="dev.projectivy.blueprint.app";
    static final class Pack {final String pkg,label;Pack(String p,String l){pkg=p;label=l;}}
    private final Context context;private final Map<String,String> mapping=new LinkedHashMap<>();
    private final Map<String,LinkedHashSet<String>> packages=new HashMap<>();
    private final LruCache<String,Drawable.ConstantState> cache=new LruCache<>(24);
    private Resources packResources;private String pack="",problem="",mappingSource="";private int exactHits,packageHits,nativeHits;
    AppArtwork(Context context){this.context=context;refresh();}
    static String component(String value){
        if(value==null)return "";String s=value.trim();if(s.startsWith("ComponentInfo{")&&s.endsWith("}"))s=s.substring(14,s.length()-1);
        ComponentName name=ComponentName.unflattenFromString(s);return name==null||name.getPackageName().isEmpty()||name.getClassName().isEmpty()?"":name.flattenToString();
    }
    static String drawable(String value){
        if(value==null)return "";
        if(value.startsWith("@drawable/")||value.startsWith("@mipmap/"))value=value.substring(value.indexOf('/')+1);
        if(value.startsWith("@")){try{Long.decode(value.substring(1));return value;}catch(NumberFormatException e){return "";}}
        return value.matches("[a-zA-Z0-9_]+")?value:"";
    }
    static Map<String,String> parse(XmlPullParser parser)throws Exception{
        Map<String,String> result=new LinkedHashMap<>();int events=0;
        for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
            if(++events>100000)throw new IOException("Icon mapping too large");
            if(event!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;
            String key=component(parser.getAttributeValue(null,"component")),image=drawable(parser.getAttributeValue(null,"drawable"));
            if(!key.isEmpty()&&!image.isEmpty())result.put(key,image);
        }
        return result;
    }
    private boolean mappingAvailable(Resources r,String pkg)throws IOException{return r.getIdentifier("appfilter","xml",pkg)!=0||Arrays.asList(r.getAssets().list("")).contains("appfilter.xml");}
    List<Pack> packs(){
        Map<String,Pack> found=new LinkedHashMap<>();PackageManager pm=context.getPackageManager();Set<String> candidates=new LinkedHashSet<>(Arrays.asList(PROJECTIVY,"tv.corebuilds.iconpack"));
        for(Intent query:Arrays.asList(new Intent("org.adw.launcher.THEMES"),new Intent("com.novalauncher.THEME"),new Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME"),new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))){
            boolean launcher=query.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)||query.hasCategory(Intent.CATEGORY_LAUNCHER);
            for(ResolveInfo r:pm.queryIntentActivities(query,0))if(r.activityInfo!=null){
                String pkg=r.activityInfo.packageName;
                // Theme declarations are authoritative even when the pack label lacks "icon".
                // For ordinary launchers, filter before resource/package reads on the UI thread.
                String label=launcher?r.loadLabel(pm).toString().toLowerCase(Locale.ROOT):"";
                if(!launcher||label.contains("icon")||label.contains("projectivy"))candidates.add(pkg);
            }
        }
        String chosen=context.getSharedPreferences(PREFS,0).getString("pack","auto");if(!chosen.isEmpty()&&!chosen.equals("auto"))candidates.add(chosen);
        for(String pkg:candidates)try{ApplicationInfo app=pm.getApplicationInfo(pkg,0);String label=app.loadLabel(pm).toString();
            Resources res=pm.getResourcesForApplication(app);if(mappingAvailable(res,pkg))found.put(pkg,new Pack(pkg,label));
        }catch(Exception ignored){}
        return new ArrayList<>(found.values());
    }
    void refresh(){
        cache.evictAll();mapping.clear();packages.clear();packResources=null;pack="";problem="";mappingSource="";exactHits=packageHits=nativeHits=0;
        String selected=context.getSharedPreferences(PREFS,0).getString("pack","auto");
        if("auto".equals(selected)){List<Pack> list=packs();selected=list.isEmpty()?"":list.get(0).pkg;for(Pack p:list)if(p.pkg.equals(PROJECTIVY)){selected=p.pkg;break;}}
        if(selected.isEmpty()){problem="Original artwork dipilih / tiada icon pack dikesan.";return;}
        try{
            // Resources only. Never include or execute another APK's code.
            Resources resources=context.createPackageContext(selected,0).getResources();Map<String,String> loaded;
            int xml=resources.getIdentifier("appfilter","xml",selected);
            if(xml!=0){try(android.content.res.XmlResourceParser parser=resources.getXml(xml)){loaded=parse(parser);}mappingSource="compiled xml/appfilter";}
            else try(InputStream in=resources.getAssets().open("appfilter.xml")){XmlPullParser parser=XmlPullParserFactory.newInstance().newPullParser();parser.setInput(in,"UTF-8");loaded=parse(parser);mappingSource="assets/appfilter.xml";}
            if(loaded.isEmpty())throw new IOException("Mapping kosong");
            pack=selected;packResources=resources;mapping.putAll(loaded);
            for(Map.Entry<String,String> item:loaded.entrySet()){String pkg=ComponentName.unflattenFromString(item.getKey()).getPackageName();packages.computeIfAbsent(pkg,k->new LinkedHashSet<>()).add(item.getValue());}
        }catch(Exception e){problem="Icon pack tidak dapat dibaca: "+e.getClass().getSimpleName()+". Pilih pack dan buka Status / refresh.";}
    }
    private Drawable image(String name){
        if(name==null||packResources==null)return null;
        try{
            int id=name.startsWith("@")?(int)Long.decode(name.substring(1)).longValue():packResources.getIdentifier(name,"drawable",pack);
            if(id==0)id=packResources.getIdentifier(name,"mipmap",pack);
            if(id==0)return null;String type=packResources.getResourceTypeName(id);if(!type.equals("drawable")&&!type.equals("mipmap"))return null;
            return packResources.getDrawable(id,null);
        }catch(Exception ignored){return null;}
    }
    static String mapped(Map<String,String> values,ResolveInfo app){
        String pkg=app.activityInfo.packageName;String found=values.get(component(pkg+"/"+app.activityInfo.name));
        if(found==null&&app.activityInfo.targetActivity!=null)found=values.get(component(pkg+"/"+app.activityInfo.targetActivity));return found;
    }
    Drawable load(ResolveInfo app){
        String key=DashboardApps.key(app);Drawable.ConstantState state=cache.get(key);if(state!=null)return state.newDrawable(context.getResources());
        Drawable value=image(mapped(mapping,app));if(value!=null)exactHits++;
        if(value==null){Set<String> aliases=packages.get(app.activityInfo.packageName);if(aliases!=null)for(String name:aliases){value=image(name);if(value!=null){packageHits++;break;}}}
        if(value==null){nativeHits++;try{value=app.activityInfo.loadBanner(context.getPackageManager());}catch(Exception ignored){}
            try{if(value==null)value=app.activityInfo.applicationInfo.loadBanner(context.getPackageManager());}catch(Exception ignored){}
            try{if(value==null)value=app.loadIcon(context.getPackageManager());}catch(Exception ignored){}
        }
        if(value==null)value=context.getPackageManager().getDefaultActivityIcon();if(value.getConstantState()!=null)cache.put(key,value.getConstantState());return value;
    }
    String status(){return "Pilihan: "+context.getSharedPreferences(PREFS,0).getString("pack","auto")+"\nPack aktif: "+(pack.isEmpty()?"tiada":pack)+"\nMapping: "+mapping.size()+" · "+mappingSource+"\nSesi semasa: exact/alias "+exactHits+", package fallback "+packageHits+", native "+nativeHits+(problem.isEmpty()?"":"\n"+problem)+"\n\nApply dalam Projectivy Launcher tidak mengubah FayyadhOS. Pilih Auto atau pack di sini. Aplikasi tiada mapping masih menggunakan artwork asal.";}
    void settings(DashboardActivity activity,Runnable changed){
        List<Pack> found=packs();String[] labels=new String[found.size()+2],values=new String[labels.length];labels[0]="Auto · Projectivy pack / TV banners";values[0]="auto";labels[1]="Original TV banners / app icons";values[1]="";
        for(int i=0;i<found.size();i++){labels[i+2]=found.get(i).label;values[i+2]=found.get(i).pkg;}
        String selected=context.getSharedPreferences(PREFS,0).getString("pack","auto");
        activity.show(new AlertDialog.Builder(activity).setTitle("Full artwork · Icon pack")
            .setSingleChoiceItems(labels,Arrays.asList(values).indexOf(selected),(d,i)->{context.getSharedPreferences(PREFS,0).edit().putString("pack",values[i]).apply();refresh();changed.run();d.dismiss();})
            .setPositiveButton("Status / refresh",(d,i)->{refresh();changed.run();activity.message("Icon pack · Diagnostik",status());})
            .setNeutralButton("Get Projectivy pack",(d,i)->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/SicMundus86/ProjectivyIconPack/releases")));}catch(Exception e){activity.message("Icon pack","github.com/SicMundus86/ProjectivyIconPack/releases");}}).setNegativeButton("Close",null));
    }
}
