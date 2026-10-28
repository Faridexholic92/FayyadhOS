package com.gathin.tvhome;
import android.content.*;
import android.content.pm.ResolveInfo;
import java.util.*;
final class DashboardApps {
 final Context c;DashboardApps(Context c){this.c=c;}
 static String key(ResolveInfo r){return new ComponentName(r.activityInfo.packageName,r.activityInfo.name).flattenToShortString();}
 List<String> order(){String s=c.getSharedPreferences("tiles",0).getString("order","");return new ArrayList<>(s.isEmpty()?Collections.emptyList():Arrays.asList(s.split("\n")));}
 List<ResolveInfo> all(){
  List<ResolveInfo> list=AppCatalog.query(c);List<String> saved=order();
  List<String> first=Arrays.asList("com.google.android.youtube.tv","com.netflix.ninja","com.amazon.amazonvideo.livingroom","org.xbmc.kodi");
  list.sort((a,b)->{int ia=saved.indexOf(key(a)),ib=saved.indexOf(key(b));
   if(ia>=0||ib>=0)return Integer.compare(ia<0?Integer.MAX_VALUE:ia,ib<0?Integer.MAX_VALUE:ib);
   int fa=first.indexOf(a.activityInfo.packageName),fb=first.indexOf(b.activityInfo.packageName);
   if(fa>=0||fb>=0)return Integer.compare(fa<0?Integer.MAX_VALUE:fa,fb<0?Integer.MAX_VALUE:fb);
   return label(a).compareToIgnoreCase(label(b));});return list;
 }
 String label(ResolveInfo r){return Cards.cleanLabel(String.valueOf(r.loadLabel(c.getPackageManager())),r.activityInfo.packageName);}
 List<ResolveInfo> visible(){List<ResolveInfo> list=all();Set<String> hidden=AppVisibility.effective(c,Config.load(c).hiddenApps);list.removeIf(r->hidden.contains(r.activityInfo.packageName));return list;}
 boolean move(String key,int delta){
  List<ResolveInfo> visible=visible();int from=-1;
  for(int i=0;i<visible.size();i++)if(key(visible.get(i)).equals(key))from=i;
  int to=from+delta;if(from<0||to<0||to>=visible.size())return false;
  ResolveInfo item=visible.remove(from);visible.add(to,item);
  Set<String> keys=new HashSet<>();for(ResolveInfo r:visible)keys.add(key(r));
  List<String> combined=new ArrayList<>();int next=0;
  for(ResolveInfo r:all())combined.add(keys.contains(key(r))?key(visible.get(next++)):key(r));
  return c.getSharedPreferences("tiles",0).edit().putString("order",android.text.TextUtils.join("\n",combined)).commit();
 }
 void reset(){c.getSharedPreferences("tiles",0).edit().remove("order").apply();}
}
