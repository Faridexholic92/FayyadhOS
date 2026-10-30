package com.gathin.tvhome;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Reference-aligned right sidebar. Native actions remain independent of optional direct Groq AI. */
final class JarvisPanel {
 private final DashboardActivity activity;private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private Dialog dialog;
 private boolean closed;private NetworkSnapshot snapshot;
 private final JarvisAiClient ai;private final java.util.function.Supplier<SpeedProbe> probeFactory;private TextView speed,speedButton,aiButton,networkStatus,ssidStatus,linkStatus,vpnStatus,aiStatus;
 private Double measuredDownload;private long measuredAt;private String measuredNetwork="";private SpeedProbe probe;private Future<?> future;
 private LinearLayout mainBox,speedBox;private ScrollView scroll;private TextView testState,testValue,testUsage,testStart,testCancel;private SpeedMeter testMeter;
 private boolean testing;private int testGeneration;private long testBytes;private String testRoute="";
 private ConnectivityManager cm;private ConnectivityManager.NetworkCallback callback;
 JarvisPanel(DashboardActivity a){this(a,new JarvisAiClient(a));}
 JarvisPanel(DashboardActivity a,JarvisAiClient client){this(a,client,SpeedProbe::new);}
 JarvisPanel(DashboardActivity a,JarvisAiClient client,java.util.function.Supplier<SpeedProbe> factory){activity=a;ai=client;probeFactory=factory;}
 private int dp(float n){return activity.d(n);}
 private float scale(){android.util.DisplayMetrics m=activity.getResources().getDisplayMetrics();return Math.min(m.widthPixels/m.density/960f,m.heightPixels/m.density/540f);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private TextView text(String s,float size,int color){return GlassUi.text(activity,s,Math.max(8,size*scale()),color);}
 private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
 private TextView button(String title,Runnable action){TextView b=text(title,11,GlassUi.WHITE);b.setGravity(Gravity.CENTER);b.setMaxLines(2);b.setPadding(dp(5),dp(5),dp(5),dp(5));GlassUi.focus(b);android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  states.addState(new int[]{-android.R.attr.state_enabled},GlassUi.surface(activity,0xff0b2330,false));b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff8095a1,GlassUi.WHITE}));
  states.addState(new int[]{android.R.attr.state_focused},GlassUi.glow(activity));states.addState(new int[]{android.R.attr.state_pressed},GlassUi.glow(activity));
  android.graphics.drawable.GradientDrawable idle=GlassUi.surface(activity,0xff062131,false);idle.setStroke(Math.max(1,dp(1)),0xff087d9e);states.addState(new int[]{},idle);b.setBackground(states);b.setOnClickListener(v->action.run());return b;}
 private void spacer(LinearLayout box,int n){View gap=new View(activity);box.addView(gap,lp(-1,dp(n)));}
 private void line(LinearLayout box){spacer(box,7);View v=new View(activity);v.setBackgroundColor(0xff16839a);box.addView(v,lp(-1,dp(1)));spacer(box,7);}
 void show(){snapshot=NetworkSnapshot.read(activity);dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout box=column();mainBox=box;box.setPadding(dp(12),dp(12),dp(12),dp(12));
  android.graphics.drawable.GradientDrawable surface=GlassUi.surface(activity,0xff041522,false);surface.setStroke(Math.max(1,dp(1)),GlassUi.CYAN);
  scroll=new ScrollView(activity);scroll.setBackground(surface);scroll.setFillViewport(true);FrameLayout pages=new FrameLayout(activity);pages.addView(box);scroll.addView(pages);dialog.setContentView(scroll);
  LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);box.addView(top);
  top.addView(new Radar(activity),lp(dp(38),dp(38)));LinearLayout heading=column();TextView title=text("Jarvis",22,GlassUi.WHITE);title.setTypeface(GlassUi.typeface(activity,Typeface.BOLD));heading.addView(title);heading.addView(text("Your Smart Assistant",10,GlassUi.MUTED));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(7);top.addView(heading,hp);TextView close=button("Close",()->close());close.setContentDescription("Close Jarvis");top.addView(close,lp(dp(42),dp(32)));
  spacer(box,12);
  actionRow(box,new String[]{"Cuaca "+new RainOptions(RainOptions.prefs(activity)).locationName,"Speedtest"},new Runnable[]{()->activity.weatherDetails(),()->openSpeedtest()});
  actionRow(box,new String[]{"Buka YouTube","Maklumat Sistem"},new Runnable[]{()->activity.youtube(),()->activity.message("Maklumat Sistem",SystemInfo.report(activity))});
  actionRow(box,new String[]{"Tetapan Wi-Fi","Aplikasi"},new Runnable[]{()->activity.system(Settings.ACTION_WIFI_SETTINGS),()->activity.installedApps()});
  line(box);networkStatus=text("",11,GlassUi.WHITE);ssidStatus=text("",11,GlassUi.WHITE);linkStatus=text("",11,GlassUi.WHITE);vpnStatus=text("",11,GlassUi.WHITE);
  box.addView(text("Status Rangkaian",12,GlassUi.WHITE));for(TextView v:new TextView[]{networkStatus,ssidStatus,linkStatus,vpnStatus}){v.setMaxLines(2);v.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams p=lp(-1,-2);p.topMargin=dp(5);box.addView(v,p);}
  line(box);aiStatus=text("",11,GlassUi.CYAN);box.addView(aiStatus);
  TextView settings=button("Jarvis AI Settings",()->{close();activity.aiSettings();});LinearLayout.LayoutParams sp=lp(-1,dp(34));sp.topMargin=dp(7);box.addView(settings,sp);
  aiButton=button("Explain with AI",()->explainNetwork());aiButton.setContentDescription("Explain native network facts using direct GroqCloud AI");LinearLayout.LayoutParams ap=lp(-1,dp(32));ap.topMargin=dp(5);box.addView(aiButton,ap);
  speed=text("Internet: not tested",11,GlassUi.WHITE);speed.setPadding(0,dp(6),0,dp(4));box.addView(speed);
  TextView detail=text("Link ≠ Internet speed. Speedtest: up to 100 MiB from Cloudflare; download only. No upload, ping or packet loss test.",9,GlassUi.MUTED);box.addView(detail);
  TextView options=button("Options",()->options());LinearLayout.LayoutParams op=lp(-1,dp(30));op.topMargin=dp(5);box.addView(options,op);
  dialog.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP){if(speedBox!=null&&speedBox.getVisibility()==View.VISIBLE)leaveSpeedtest();else close();}return true;});
  dialog.setOnDismissListener(d->release());dialog.show();activity.overlayOpened(dialog);Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(0f);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
   android.util.DisplayMetrics m=activity.getResources().getDisplayMetrics();int margin=dp(16);window.setLayout(Math.min(m.widthPixels-margin*2,dp(222)),Math.min(m.heightPixels-margin*2,dp(506)));
   WindowManager.LayoutParams p=window.getAttributes();p.gravity=Gravity.RIGHT|Gravity.TOP;p.x=dp(10);p.y=margin;window.setAttributes(p);}
  updateNetwork();refreshAiStatus();watchNetwork();speedButton.requestFocus();

 }
 private void actionRow(LinearLayout box,String[] labels,Runnable[] actions){
  LinearLayout row=new LinearLayout(activity);LinearLayout.LayoutParams rp=lp(-1,dp(40));rp.bottomMargin=dp(6);box.addView(row,rp);
  for(int i=0;i<2;i++){TextView b=button(labels[i],actions[i]);
   int kind=labels[i].startsWith("Cuaca")?0:labels[i].equals("Speedtest")?1:labels[i].equals("Buka YouTube")?2:labels[i].equals("Maklumat Sistem")?3:labels[i].equals("Tetapan Wi-Fi")?4:5;
   android.graphics.drawable.Drawable icon=kind==0?activity.getDrawable(R.drawable.ic_weather_cloudy):kind==3?activity.getDrawable(R.drawable.ic_settings):kind==4?activity.getDrawable(R.drawable.ic_wifi):new ActionGlyph(kind);
   if(icon!=null){icon.setBounds(0,0,dp(16),dp(16));if(kind==4)icon.setTint(GlassUi.CYAN);b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(3));}
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(i==0)p.rightMargin=dp(6);row.addView(b,p);if(labels[i].equals("Speedtest"))speedButton=b;}
 }
 private void refreshAiStatus(){JarvisKeyStore store=new JarvisKeyStore(activity);aiStatus.setText(activity.getString(R.string.jarvis_direct_ai_status,store.configured()?"Key configured":"No key configured",store.model().isEmpty()?"Not selected":store.model(),store.testedAt()==0?"Connection not tested":"Previous test succeeded; not a live status"));}
 private void updateNetwork(){if(closed)return;snapshot=NetworkSnapshot.read(activity);
  if(testing&&!testRoute.equals(snapshot.networkKey)){cancelTest();testState.setText("Rangkaian berubah. Sila uji semula.");speed.setText(R.string.jarvis_direct_route_changed);}
  if(measuredDownload!=null&&!measuredNetwork.equals(snapshot.networkKey)){measuredDownload=null;speed.setText(R.string.jarvis_direct_network_changed);}
  networkStatus.setText(activity.getString(R.string.jarvis_direct_network_status,snapshot.transport,snapshot.validated?"Internet validated by Android":"Internet not validated"));
  ssidStatus.setText(activity.getString(R.string.jarvis_direct_wifi_status,snapshot.ssid,snapshot.rssi<0?activity.getString(R.string.jarvis_direct_signal_dbm,snapshot.rssi):""));
  linkStatus.setText(activity.getString(R.string.jarvis_direct_link_status,snapshot.link>0?snapshot.link+" Mbps":"Unavailable",snapshot.band));
  vpnStatus.setText(activity.getString(R.string.jarvis_direct_vpn_status,snapshot.vpn?"Detected":"Not detected"));
 }
 private void watchNetwork(){try{cm=(ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);if(cm==null)return;callback=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){handler.post(()->updateNetwork());}public void onLost(Network n){handler.post(()->updateNetwork());}public void onCapabilitiesChanged(Network n,NetworkCapabilities c){handler.post(()->updateNetwork());}};cm.registerDefaultNetworkCallback(callback);}catch(RuntimeException e){callback=null;}}
 private void busy(boolean value){speedButton.setEnabled(!value);aiButton.setEnabled(!value);if(value){speed.setFocusable(true);speed.requestFocus();}else{speed.setFocusable(false);speedButton.requestFocus();}}
 private void openSpeedtest(){
  if(closed||future!=null&&!future.isDone())return;
  if(speedBox==null){
   speedBox=column();speedBox.setPadding(dp(12),dp(12),dp(12),dp(12));((FrameLayout)mainBox.getParent()).addView(speedBox);
   LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);speedBox.addView(top);
   top.addView(new Radar(activity),lp(dp(38),dp(38)));TextView heading=text("Jarvis",22,GlassUi.WHITE);heading.setTypeface(GlassUi.typeface(activity,Typeface.BOLD));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=dp(7);top.addView(heading,hp);top.addView(button("Close",()->close()),lp(dp(42),dp(32)));
   spacer(speedBox,12);speedBox.addView(button("‹ Diagnostik",()->leaveSpeedtest()),lp(-1,dp(30)));line(speedBox);
   TextView title=text("Speedtest",18,GlassUi.WHITE);title.setTypeface(GlassUi.typeface(activity,Typeface.BOLD));speedBox.addView(title);
   testState=text("Sedia untuk diuji",11,GlassUi.CYAN);spacer(speedBox,6);speedBox.addView(testState);
   FrameLayout meterFrame=new FrameLayout(activity);testMeter=new SpeedMeter(activity);meterFrame.addView(testMeter,new FrameLayout.LayoutParams(-1,-1));LinearLayout.LayoutParams mp=lp(dp(118),dp(118));mp.gravity=Gravity.CENTER_HORIZONTAL;mp.topMargin=dp(10);speedBox.addView(meterFrame,mp);
   testValue=text("—\nMbps",20,GlassUi.WHITE);testValue.setGravity(Gravity.CENTER);meterFrame.addView(testValue,new FrameLayout.LayoutParams(-1,-1));
   TextView label=text("Kelajuan muat turun",11,GlassUi.WHITE);label.setGravity(Gravity.CENTER);speedBox.addView(label);spacer(speedBox,12);
   testUsage=text("0 / 100 MiB",12,GlassUi.WHITE);speedBox.addView(testUsage);
   speedBox.addView(text("Had 100 MiB · Download sahaja · Cloudflare",9,GlassUi.MUTED));spacer(speedBox,10);
   testStart=button("Mula Speedtest",()->startTest());speedBox.addView(testStart,lp(-1,dp(34)));spacer(speedBox,8);
   testCancel=button("Batal ujian",()->cancelTest());speedBox.addView(testCancel,lp(-1,dp(30)));testCancel.setEnabled(false);spacer(speedBox,12);
   speedBox.addView(text("Had 120 saat. Ujian boleh mengganggu streaming. Anggaran satu sambungan, termasuk masa sambungan.",9,GlassUi.MUTED));
  }
  mainBox.setVisibility(View.GONE);speedBox.setVisibility(View.VISIBLE);scroll.scrollTo(0,0);testStart.requestFocus();
 }
 private void leaveSpeedtest(){cancelTest();speedBox.setVisibility(View.GONE);mainBox.setVisibility(View.VISIBLE);scroll.scrollTo(0,0);speedButton.requestFocus();}
 private void cancelTest(){
  if(!testing)return;testing=false;testGeneration++;if(probe!=null)probe.cancel();if(future!=null)future.cancel(true);
  testState.setText("Ujian dibatalkan");testValue.setText("—\nMbps");speed.setText("Speedtest dibatalkan; tiada keputusan baharu.");finishTestUi();
 }
 private void finishTestUi(){testStart.setEnabled(true);testStart.setText("Mula Speedtest");testCancel.setEnabled(false);speedButton.setEnabled(true);aiButton.setEnabled(true);if(speedBox.getVisibility()==View.VISIBLE)testStart.requestFocus();}
 private void startTest(){
  if(closed||testing||future!=null&&!future.isDone())return;updateNetwork();testRoute=snapshot.networkKey;
  final String route=testRoute;final int generation=++testGeneration;final SpeedProbe current=probeFactory.get();probe=current;testing=true;testBytes=0;measuredDownload=null;
  testState.setText("Sedang menguji…");testValue.setText("—\nMbps");testUsage.setText("0 / 100 MiB");testMeter.progress=0;testMeter.invalidate();testStart.setText("Sedang menguji…");testStart.setEnabled(false);testCancel.setEnabled(true);testCancel.requestFocus();speedButton.setEnabled(false);aiButton.setEnabled(false);speed.setText("Measuring download…");
  future=worker.submit(()->{
   try{double result=current.run((bytes,seconds)->handler.post(()->{
    if(closed||generation!=testGeneration||!testing)return;testBytes=bytes;testUsage.setText(String.format(Locale.ENGLISH,"%.1f / 100 MiB",bytes/1048576.0));testMeter.progress=(float)bytes/SpeedProbe.LIMIT;testMeter.invalidate();
    testValue.setText(String.format(Locale.ENGLISH,"%.1f\nMbps",SpeedProbe.mbps(bytes,seconds)));
   }));handler.post(()->{
    if(closed||generation!=testGeneration)return;testing=false;updateNetwork();finishTestUi();
    if(!route.equals(snapshot.networkKey)){testState.setText("Rangkaian berubah. Sila uji semula.");testValue.setText("—\nMbps");speed.setText(R.string.jarvis_direct_route_changed);return;}
    measuredDownload=result;measuredAt=System.currentTimeMillis();measuredNetwork=route;
    testBytes=SpeedProbe.LIMIT;testUsage.setText("100 / 100 MiB");testMeter.progress=1;testMeter.invalidate();testState.setText("Selesai · Anggaran download");testValue.setText(String.format(Locale.ENGLISH,"%.1f\nMbps",result));speed.setText(activity.getString(R.string.jarvis_direct_download_estimate,String.format(Locale.ENGLISH,"%.1f",result)));
   });}catch(Exception e){handler.post(()->{if(closed||generation!=testGeneration)return;testing=false;measuredDownload=null;testState.setText("Ujian gagal / tamat masa. Cuba semula.");testValue.setText("—\nMbps");speed.setText(R.string.jarvis_direct_download_failed);finishTestUi();});}
  });
 }
 private void explainNetwork(){
  if(closed||future!=null&&!future.isDone())return;updateNetwork();
  if(measuredDownload!=null&&(System.currentTimeMillis()-measuredAt<0||System.currentTimeMillis()-measuredAt>5*60_000L))measuredDownload=null;
  final org.json.JSONObject context;
  try{context=new org.json.JSONObject().put("transport",snapshot.transport).put("validated",snapshot.validated).put("vpn",snapshot.vpn).put("linkMbps",snapshot.link);
   if(measuredDownload!=null)context.put("estimatedDownloadMbps",measuredDownload).put("downloadMeasuredAt",java.time.Instant.ofEpochMilli(measuredAt).toString());
  }catch(org.json.JSONException e){speed.setText(R.string.jarvis_ai_preparation_error);return;}
  busy(true);speed.setText(R.string.jarvis_ai_loading);
  future=worker.submit(()->{String response;try{response="GroqCloud AI · "+ai.explain(context);}catch(Exception e){response="GroqCloud unavailable · "+JarvisAiClient.safeError(e);}final String result=response;
   handler.post(()->{if(closed)return;speed.setText(result);busy(false);refreshAiStatus();});});
 }
 private void options(){close();activity.show(new AlertDialog.Builder(activity).setTitle("Jarvis · Options").setItems(new String[]{"Allow Wi-Fi name","Tentang Speedtest","Wi-Fi settings","Network / LAN settings","Light boost","Restore before Boost",activity.startupGreetingEnabled()?"Startup greeting: ON":"Startup greeting: OFF","Play greeting"},(d,i)->{
  if(i==0){if(activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},72);else activity.message("Wi-Fi name","Location permission is granted. Some firmware also requires Location services. Jarvis does not read GPS coordinates.");}
  else if(i==1)activity.message("Speedtest","Download sahaja, maksimum 100 MiB dari Cloudflare. Tiada upload. Bukan Ookla. Keputusan ialah anggaran satu sambungan, bukan Wi-Fi link speed.");else if(i==2)activity.system(Settings.ACTION_WIFI_SETTINGS);else if(i==3)activity.system(Settings.ACTION_WIRELESS_SETTINGS);
  else if(i==4)activity.show(new AlertDialog.Builder(activity).setTitle("Enable light boost?").setMessage("Pause this launcher's automatic prayer and Places refresh only. Cached data and manual refresh remain. No DNS, router, VPN or bandwidth changes; weather still follows its own refresh policy.").setPositiveButton("Enable",(x,w)->{NetworkFocus.enable(activity);activity.modeChanged();}).setNegativeButton("Cancel",null));
  else if(i==5)activity.show(new AlertDialog.Builder(activity).setTitle("Restore before Boost?").setMessage("Restore saved launcher refresh preferences, not system network settings.").setPositiveButton("Restore",(x,w)->{boolean restored=NetworkFocus.restore(activity);activity.modeChanged();if(!restored)activity.message("Restore","No complete snapshot is available. No settings were changed.");}).setNegativeButton("Cancel",null));
  else if(i==6){activity.toggleStartupGreeting();activity.showJarvis();}else if(i==7)activity.replayWelcome();
 }).setNegativeButton("Close",null));}
 void close(){if(dialog!=null&&dialog.isShowing())dialog.dismiss();release();}
 private void release(){if(closed)return;closed=true;testGeneration++;testing=false;if(callback!=null&&cm!=null){try{cm.unregisterNetworkCallback(callback);}catch(RuntimeException ignored){}callback=null;}activity.overlayClosed(dialog);handler.removeCallbacksAndMessages(null);ai.cancel();if(probe!=null)probe.cancel();if(future!=null)future.cancel(true);worker.shutdownNow();}
 private static final class ActionGlyph extends android.graphics.drawable.Drawable {
  private final int kind;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);ActionGlyph(int k){kind=k;}
  public void draw(Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(GlassUi.CYAN);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);
   if(kind==1){canvas.drawArc(2,2,22,22,155,230,false,p);canvas.drawLine(12,12,18,6,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(12,12,2,p);}
   else if(kind==2){p.setStyle(Paint.Style.FILL);p.setColor(0xffff1616);canvas.drawRoundRect(1,4,23,20,4,4,p);p.setColor(Color.WHITE);Path play=new Path();play.moveTo(10,8);play.lineTo(17,12);play.lineTo(10,16);play.close();canvas.drawPath(play,p);}
   else{for(int x=3;x<=14;x+=11)for(int y=3;y<=14;y+=11)canvas.drawRoundRect(x,y,x+7,y+7,1,1,p);}canvas.restore();
  }
  public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
 }
 private static final class SpeedMeter extends View {
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);float progress;
  SpeedMeter(Context c){super(c);setContentDescription("Kemajuan download Speedtest");}
  protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.85f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xff173b4b);c.drawCircle(x,y,r,p);p.setColor(GlassUi.CYAN);p.setStrokeCap(Paint.Cap.ROUND);c.drawArc(x-r,y-r,x+r,y+r,-90,Math.max(3,360*progress),false,p);}
 }
 private static final class Radar extends View {
  private final Paint p=new Paint(3);private float angle=-80;private boolean active;
  private final Runnable frame=new Runnable(){public void run(){if(!active)return;angle=(angle+3)%360;invalidate();postDelayed(this,50);}};
  Radar(Context c){super(c);setContentDescription("Jarvis animated assistant emblem");}
  public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);removeCallbacks(frame);active=visible&&android.animation.ValueAnimator.areAnimatorsEnabled();if(active)postDelayed(frame,50);}
  protected void onDetachedFromWindow(){active=false;removeCallbacks(frame);super.onDetachedFromWindow();}
  protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.83f;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff16839a);c.drawCircle(x,y,r,p);c.drawCircle(x,y,r*.72f,p);p.setStrokeWidth(4);p.setColor(GlassUi.CYAN);c.drawArc(x-r,y-r,x+r,y+r,angle,285,false,p);}
 }
}
