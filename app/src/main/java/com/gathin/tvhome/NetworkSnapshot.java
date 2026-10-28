package com.gathin.tvhome;
import android.content.Context;
import android.net.*;
import android.net.wifi.*;
import java.util.*;
final class NetworkSnapshot {
 String transport="No connection",ssid="Unavailable",band="Unavailable";
 boolean validated,vpn;int link=-1,frequency=-1,rssi=0;String networkKey="";
 static String band(int mhz){return mhz>=2400&&mhz<=2500?"2.4 GHz":mhz>=4900&&mhz<5925?"5 GHz":mhz>=5925&&mhz<=7125?"6 GHz":"Unavailable";}
 static String cleanSsid(String raw){
  if(raw==null||raw.equals("<unknown ssid>")||raw.isEmpty())return "Unavailable";
  if(raw.startsWith("\"")&&raw.endsWith("\"")&&raw.length()>=2)raw=raw.substring(1,raw.length()-1);
  raw=raw.replaceAll("[\\p{Cntrl}\\u202a-\\u202e\\u2066-\\u2069]","");
  return raw.isEmpty()?"Unavailable":raw.substring(0,Math.min(raw.length(),64));
 }
 static NetworkSnapshot read(Context context){NetworkSnapshot s=new NetworkSnapshot();
  try{ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
   Network active=cm==null?null:cm.getActiveNetwork();NetworkCapabilities ac=active==null?null:cm.getNetworkCapabilities(active);
   if(ac==null)return s;s.networkKey=active.toString();s.validated=ac.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);s.vpn=ac.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
   boolean wifi=ac.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),lan=ac.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
   if(s.vpn){for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c==null||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))continue;wifi|=c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);lan|=c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);}}
   s.transport=wifi&&lan?"Wi-Fi / LAN":wifi?"Wi-Fi":lan?"LAN / Ethernet":s.vpn?"VPN":"Other network";
   if(wifi){try{WifiManager wm=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);WifiInfo info=wm==null?null:wm.getConnectionInfo();if(info!=null){s.ssid=cleanSsid(info.getSSID());int signal=info.getRssi();if(signal>-127&&signal<0)s.rssi=signal;s.frequency=info.getFrequency();s.band=band(s.frequency);s.link=info.getLinkSpeed()>0?info.getLinkSpeed():-1;}}catch(RuntimeException ignored){}}
  }catch(RuntimeException ignored){}return s;
 }
 String speech(){return "Network report. Connection: "+transport+(vpn?", with VPN":"")+". "+(validated?"Internet validated by Android.":"Internet not yet validated by Android.")+
  (transport.contains("Wi-Fi")?" Wi-Fi name: "+ssid+". Band: "+band+". "+(link>0?"Link speed: "+link+" megabits per second. This is not an Internet speed test.":"Link speed unavailable."):"")+" For an estimate of Internet download speed, select Test Internet.";}
}
