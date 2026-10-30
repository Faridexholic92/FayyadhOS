package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Responsive remote-first glass home. Real installed apps only; no placeholder shortcuts. */
public class DashboardActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private WeatherForecast.State wallpaperState=WeatherForecast.State.UNKNOWN;
 private String wallpaperKey="";
 private WallpaperDissolveView wallpaper;private HourlyWeatherController hourly;private boolean pendingDim;private TextView clock,date,greeting;private RecyclerView grid;
 private PrayerPanel prayer;private DashboardApps catalog;private List<ResolveInfo> apps=new ArrayList<>();
 private String moving,focusKey,lastFocusedKey;private boolean swallowOK;private float scale=1;
 private TextView empty;
 RainController rain;
 private WelcomeSpeech welcome;private boolean resumed,screenReceiverRegistered;
 private final Runnable welcomeTask=()->{if(resumed&&welcome!=null)welcome.speak(false);};
 private final BroadcastReceiver screenReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
  if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){if(rain!=null)rain.screen(false);if(hourly!=null)hourly.screen(false);wallpaper.hostActive(false);handler.removeCallbacks(welcomeTask);welcome.screenOff();}
  else if(Intent.ACTION_SCREEN_ON.equals(i.getAction())){if(rain!=null)rain.screen(true);if(resumed){wallpaper.hostActive(true);if(hourly!=null)hourly.screen(true);}if(resumed)scheduleWelcome();}
 }};
 private void scheduleWelcome(){handler.removeCallbacks(welcomeTask);handler.postDelayed(welcomeTask,1200);}
 private JarvisPanel jarvis;private AlertDialog popup;private FrameLayout root;
 private final Map<Dialog,View> overlays=new LinkedHashMap<>();
 private View overlayReturnFocus;
 private final Runnable tick=new Runnable(){public void run(){updateClock();handler.postDelayed(this,30000);}};
 int d(float n){return GlassUi.dp(this,n*scale);}
 static String greeting(int hour){return hour>=5&&hour<12?"Good morning":hour<14&&hour>=12?"Good afternoon":hour>=14&&hour<19?"Good evening":"Good night";}
 private TextView text(String s,float size,int color){TextView t=GlassUi.text(this,s,Math.max(8,size*scale),color);t.setShadowLayer(GlassUi.dp(this,2),0,GlassUi.dp(this,1),0xd9000000);return t;}
 private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  welcome=new WelcomeSpeech(this);IntentFilter screenEvents=new IntentFilter();screenEvents.addAction(Intent.ACTION_SCREEN_OFF);screenEvents.addAction(Intent.ACTION_SCREEN_ON);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,screenEvents,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screenReceiver,screenEvents);
  screenReceiverRegistered=true;
  float w=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;
  float h=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density;
  scale=Math.min(w/960f,h/540f);catalog=new DashboardApps(this);
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
  root=new FrameLayout(this);root.setId(R.id.root);root.setBackgroundColor(0xff0b1923);setContentView(root);
  wallpaper=new WallpaperDissolveView(this);wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
  loadWallpaper();
  // A uniform 960x540 design space keeps the reference composition at 720p/1080p/4K.
  FrameLayout body=new FrameLayout(this);body.setBackground(new android.graphics.drawable.GradientDrawable(
   android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x45071320,0x08071320,0x28071320}));
  root.addView(body,new FrameLayout.LayoutParams(-1,-1));
  FrameLayout header=new FrameLayout(this);place(body,header,40,28,880,62);
  LinearLayout brand=vertical();TextView name=text("FayyadhOS",30,GlassUi.WHITE);
  android.text.SpannableString wordmark=new android.text.SpannableString("FayyadhOS");
  wordmark.setSpan(new android.text.style.ForegroundColorSpan(GlassUi.CYAN),7,9,0);name.setText(wordmark);
  name.setTypeface(null,Typeface.BOLD);brand.addView(name);
  TextView edition=text("KIDS UNIVERSE  ·  BOBOIBOY × PAPAZOLA",10,GlassUi.WHITE);edition.setLetterSpacing(.38f);edition.setPadding(d(16),d(3),0,0);brand.addView(edition);
  header.addView(brand,new FrameLayout.LayoutParams(d(230),-2,Gravity.START|Gravity.TOP));
  LinearLayout time=vertical();time.setGravity(Gravity.CENTER_HORIZONTAL);
  clock=text("",34,GlassUi.WHITE);clock.setTypeface(null,Typeface.BOLD);date=text("",13,GlassUi.WHITE);
  clock.setGravity(Gravity.CENTER);date.setGravity(Gravity.CENTER);date.setSingleLine(true);
  date.setEllipsize(android.text.TextUtils.TruncateAt.END);time.addView(clock);time.addView(date);
  header.addView(time,new FrameLayout.LayoutParams(d(310),-2,Gravity.CENTER_HORIZONTAL|Gravity.TOP));
  LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
  TextView net=button("●  Jarvis",()->showJarvis());net.setContentDescription("Open Jarvis network diagnostics and AI assistance");net.setId(R.id.btnWifi);actions.addView(net,lp(d(82),d(44)));
  View divider=new View(this);divider.setBackgroundColor(0x66ffffff);actions.addView(divider,lp(d(1),d(24)));
  TextView settings=button("Settings",()->settings());android.graphics.drawable.Drawable gear=getDrawable(R.drawable.ic_settings);gear.setBounds(0,0,d(18),d(18));settings.setCompoundDrawables(gear,null,null,null);
  settings.setCompoundDrawablePadding(d(5));settings.setPadding(d(10),0,d(5),0);settings.setId(R.id.btnSettings);actions.addView(settings,lp(d(100),d(44)));
  header.addView(actions,new FrameLayout.LayoutParams(-2,d(44),Gravity.END|Gravity.TOP));
  LinearLayout intro=vertical();place(body,intro,40,128,310,100);
  greeting=text("",32,GlassUi.WHITE);greeting.setTypeface(null,Typeface.BOLD);greeting.setTag("greeting");intro.addView(greeting);
  TextView caption=text("Dunia pengembaraan bermula di sini!",18,GlassUi.WHITE);LinearLayout.LayoutParams cap=lp(-1,-2);cap.topMargin=d(4);intro.addView(caption,cap);
  TextView heroes=text("BOBOIBOY  ✦  PAPAZOLA",13,0xffffd45c);heroes.setTypeface(null,Typeface.BOLD);heroes.setLetterSpacing(.08f);place(body,heroes,40,224,310,24);
  grid=new LoopCarousel(this);grid.setId(R.id.grid);grid.setClipToPadding(false);grid.setPadding(d(3),d(6),d(3),d(6));
  grid.setItemAnimator(null);
  grid.setOverScrollMode(View.OVER_SCROLL_NEVER);place(body,grid,40,400,880,112);
  empty=text("No visible apps. Open Settings to unhide apps or install TV apps.",16,GlassUi.WHITE);empty.setGravity(Gravity.CENTER_VERTICAL);place(body,empty,46,406,868,96);
  prayer=new PrayerPanel(this);prayer.dashboardScale(scale);place(body,prayer,40,84,390,40);
  prayer.setNextFocusUpId(R.id.btnWifi);prayer.setNextFocusDownId(R.id.grid);
  net.setNextFocusDownId(R.id.prayerPanel);settings.setNextFocusDownId(R.id.prayerPanel);
  net.setNextFocusRightId(R.id.btnSettings);settings.setNextFocusLeftId(R.id.btnWifi);

  net.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&k==KeyEvent.KEYCODE_DPAD_DOWN){prayer.requestFocus();return true;}return false;});settings.setOnKeyListener((v,k,e)->{if(e.getAction()==KeyEvent.ACTION_DOWN&&k==KeyEvent.KEYCODE_DPAD_DOWN){prayer.requestFocus();return true;}return false;});
  prayer.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
   if(key==KeyEvent.KEYCODE_DPAD_UP){net.requestFocus();return true;}
   if(key==KeyEvent.KEYCODE_DPAD_DOWN){if(apps.isEmpty())settings.requestFocus();else focusApp(lastFocusedKey!=null?lastFocusedKey:DashboardApps.key(apps.get(0)));return true;}
   if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)return prayer.cycle(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return false;});
  // Structural layout only: no weather surface/background, focus target or click handler.
  // Weather sits below the top-right actions, outside the centered clock.
   WeatherWidget weather=new WeatherWidget(this,scale);place(body,weather,690,96,230,38);
  rain=new RainController(this,root);rain.attachWeather(weather);
  hourly=new HourlyWeatherController(this);place(body,hourly.view(),690,140,230,78);
  reload();updateClock();
  if(!apps.isEmpty()){focusKey=DashboardApps.key(apps.get(0));grid.post(()->focusApp(focusKey));}else settings.requestFocus();
 }
 private void place(FrameLayout parent,View child,float x,float y,float width,float height){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(d(width),d(height));p.leftMargin=d(x);p.topMargin=d(y);parent.addView(child,p);}
 private void focusApp(String key){
  if(apps.isEmpty()){findViewById(R.id.btnSettings).requestFocus();return;}
  int target=0;for(int i=0;i<apps.size();i++)if(DashboardApps.key(apps.get(i)).equals(key)){target=i;break;}
  int position=moving!=null?target:LoopCarousel.anchor(target,apps.size());
  ((LoopCarousel)grid).center(position,false);
 }


 private TextView button(String label,Runnable click){TextView t=text(label,13,GlassUi.WHITE);t.setGravity(Gravity.CENTER);t.setSingleLine(true);GlassUi.focusShortcut(t);t.setOnClickListener(v->click.run());return t;}
 void updateClock(){LocalDateTime now=LocalDateTime.now();clock.setText(now.format(DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(Locale.getDefault())));date.setText(now.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy",Locale.getDefault())));greeting.setText("Jom Teroka!");}
 boolean automaticWallpaper(){return getSharedPreferences("fayyadh-wallpaper",0).getBoolean("automatic_weather",true);}
 void weatherWallpaper(WeatherForecast.State state){wallpaperState=state;loadWallpaper();}
 void setAutomaticWallpaper(boolean enabled){getSharedPreferences("fayyadh-wallpaper",0).edit().putBoolean("automatic_weather",enabled).apply();loadWallpaper();}
 String currentWallpaperKey(){return wallpaperKey;}
 private void loadWallpaper(){
  String scene=automaticWallpaper()?WeatherWallpapers.asset(wallpaperState):null;
  String selected=getSharedPreferences("fayyadh-wallpaper",0).getString("selected",FayyadhWallpapers.DEFAULT);
  if(!FayyadhWallpapers.contains(selected))selected=FayyadhWallpapers.DEFAULT;
  String key=scene!=null?scene:"manual:"+selected;if(key.equals(wallpaperKey))return;
  final String asset=scene,chosen=selected;
  wallpaperKey=key;
  wallpaper.request(key,()->{
   Bitmap bitmap=null;
   if(asset!=null)try(java.io.InputStream in=getAssets().open(asset)){bitmap=decodeWallpaper(in);}catch(java.io.IOException ignored){}
   if(bitmap==null){File file=FayyadhWallpapers.file(this,chosen);if(file.isFile())try(java.io.InputStream in=new java.io.FileInputStream(file)){bitmap=decodeWallpaper(in);}catch(java.io.IOException ignored){}}
   return bitmap;
  });
 }
 private Bitmap decodeWallpaper(java.io.InputStream input)throws java.io.IOException {
  // Bundled assets are trusted. Bound decode size for TV memory; never keep a pending decoded bitmap.
  byte[] bytes;try(java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
  BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
  options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1920||options.outHeight/options.inSampleSize>1080)options.inSampleSize*=2;
  options.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
 }
 void wallpapers(){String selected=getSharedPreferences("fayyadh-wallpaper",0).getString("selected",FayyadhWallpapers.DEFAULT);
  show(new AlertDialog.Builder(this).setTitle("Wallpaper · Ikut cuaca: "+(automaticWallpaper()?"ON":"OFF"))
   .setSingleChoiceItems(FayyadhWallpapers.NAMES,Arrays.asList(FayyadhWallpapers.FILES).indexOf(selected),(d,i)->{
    getSharedPreferences("fayyadh-wallpaper",0).edit().putString("selected",FayyadhWallpapers.FILES[i]).putBoolean("automatic_weather",false).apply();loadWallpaper();d.dismiss();})
   .setNeutralButton(automaticWallpaper()?"Cuaca auto: OFF":"Cuaca auto: ON",(d,i)->{setAutomaticWallpaper(!automaticWallpaper());})
   .setNegativeButton("Close",null));}
 void settings(){handler.removeCallbacks(welcomeTask);welcome.cancel();show(new AlertDialog.Builder(this).setTitle("FayyadhOS · Settings").setItems(new String[]{"Wallpaper","Hide / unhide apps","Rearrange apps","Jarvis · Network & AI","Prayer zone & options","Android settings","Set default Home",getString(R.string.rain_settings_entry),"Azan · Lima waktu","Cuaca · Sekarang / +3j / +6j",DimService.enabled(this)?"Dim 35%: ON":"Dim 35%: OFF"},(d,i)->{
  if(i==10){toggleDim();}else if(i==9){d.dismiss();handler.post(()->hourly.settings());}else if(i==8){startActivity(new Intent(this,AzanSettingsActivity.class));}else if(i==0)wallpapers();else if(i==1)hidden();else if(i==2)message("Rearrange apps","Hold OK on an app > Move. Use the arrow keys to rearrange. Press OK or Back to finish. Your order is saved automatically.");else if(i==3)showJarvis();else if(i==4)prayer.performClick();else if(i==7){d.dismiss();handler.post(()->rain.showSettings());}else system(i==6?Settings.ACTION_HOME_SETTINGS:Settings.ACTION_SETTINGS);
 }).setNegativeButton("Close",null));}
 void toggleDim(){
  if(DimService.enabled(this)){DimService.disable(this);Toast.makeText(this,"Dim OFF",Toast.LENGTH_SHORT).show();return;}
  if(!Settings.canDrawOverlays(this)){
   show(new AlertDialog.Builder(this).setTitle("Dim 35% · permission").setMessage("Allow display over other apps. The dim layer does not take remote focus or touches. A persistent service notification offers OFF. Some protected/system screens may exclude overlays. This is not panel brightness control.")
    .setPositiveButton("Open permission",(dialog,which)->{try{pendingDim=true;startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,android.net.Uri.parse("package:"+getPackageName())));}catch(Exception unsupported){pendingDim=false;message("Dim","This firmware does not expose overlay permission. Dim stays OFF.");}}).setNegativeButton("Cancel",null));
  }else startDim();
 }
 private void startDim(){
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){pendingDim=true;requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},73);return;}
  if(!DimService.enable(this))message("Dim","Overlay service could not start. Dim remains OFF.");
 }
 private void restoreDim(){
  if(pendingDim){pendingDim=false;if(Settings.canDrawOverlays(this))startDim();}
  else if(DimService.enabled(this)){if(Settings.canDrawOverlays(this))DimService.enable(this);else DimService.disable(this);}
 }
 void hidden(){List<ResolveInfo> all=catalog.all();Set<String> hidden=AppVisibility.effective(this,Config.load(this).hiddenApps);Map<String,Boolean> choice=new HashMap<>();String[] names=new String[all.size()];boolean[] checked=new boolean[all.size()];
  for(int i=0;i<all.size();i++){String pkg=all.get(i).activityInfo.packageName;names[i]=catalog.label(all.get(i));checked[i]=hidden.contains(pkg);choice.put(pkg,checked[i]);}
  show(new AlertDialog.Builder(this).setTitle("Hide apps · ✓ = hidden").setMultiChoiceItems(names,checked,(d,i,on)->choice.put(all.get(i).activityInfo.packageName,on)).setPositiveButton("Save",(d,i)->{AppVisibility.save(this,choice);reload();}).setNegativeButton("Cancel",null));}
 void showJarvis(){handler.removeCallbacks(welcomeTask);welcome.cancel();if(jarvis!=null)jarvis.close();jarvis=new JarvisPanel(this);jarvis.show();}
 void system(String action){try{startActivity(new Intent(action));}catch(Exception e){message("Settings","This page is not provided by your TV firmware. Use the original system settings.");}}
 boolean startupGreetingEnabled(){return welcome.enabled();}
 void toggleStartupGreeting(){welcome.setEnabled(!welcome.enabled());}
 void replayWelcome(){handler.removeCallbacks(welcomeTask);welcome.speak(true);}
 void modeChanged(){prayer.modeChanged();}
 void aiSettings(){new JarvisAiSettings(this).show();}
 void weatherDetails(){message("Cuaca · Weather",rain.status()+(hourly==null?"":hourly.status()));}
 void installedApps(){List<ResolveInfo> installed=catalog.visible();String[] labels=new String[installed.size()];for(int i=0;i<labels.length;i++)labels[i]=catalog.label(installed.get(i));
  if(labels.length==0){message("Applications","No visible apps. Use Settings to unhide apps.");return;}
  show(new AlertDialog.Builder(this).setTitle("Installed applications").setItems(labels,(d,i)->open(installed.get(i))).setNegativeButton("Close",null));
 }
 void youtube(){for(ResolveInfo app:catalog.all())if(app.activityInfo.packageName.equals("com.google.android.youtube.tv")||app.activityInfo.packageName.equals("com.google.android.youtube")){open(app);return;}message("YouTube","No launchable YouTube app was discovered on this device.");}

 // Overlay windows must never change the home layer's visibility or geometry.
 // Track owners, not a boolean: an old dialog may dismiss after its replacement opens.
 void overlayOpened(Dialog dialog){
  if(overlays.containsKey(dialog))return;
  if(overlays.isEmpty())overlayReturnFocus=root.findFocus();
  overlays.put(dialog,root.findFocus());
  if(rain!=null)rain.obscured(true);if(prayer!=null)prayer.obscured(true);
 }
 void overlayClosed(Dialog dialog){
  if(!overlays.containsKey(dialog))return;
  overlays.remove(dialog);
  if(rain!=null)rain.obscured(!overlays.isEmpty());if(prayer!=null)prayer.obscured(!overlays.isEmpty());
  if(!overlays.isEmpty())return;
  View target=overlayReturnFocus;overlayReturnFocus=null;
  if(moving!=null)focusApp(moving);
  else if(target!=null&&target.isShown()&&target.isFocusable()&&target.getRootView()==root.getRootView())target.requestFocus();
  else findViewById(R.id.btnSettings).requestFocus();
 }
 AlertDialog show(AlertDialog.Builder b){return show(b,true);}
 AlertDialog show(AlertDialog.Builder b,boolean styled){
  AlertDialog shown=b.create();popup=shown;
  shown.setOnKeyListener((dialog,key,event)->{if(key!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_UP)shown.cancel();return true;});
  shown.setOnDismissListener(d->{overlayClosed(shown);if(popup==shown)popup=null;});
  shown.show();overlayOpened(shown);if(styled)GlassUi.style(shown);return shown;
 }
 void message(String title,String body){show(new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("Close",null));}
 private void reload(){if(grid.getLayoutManager()!=null)grid.getLayoutManager().removeAllViews();apps=catalog.visible();grid.setAdapter(new Tiles());empty.setVisibility(apps.isEmpty()?View.VISIBLE:View.GONE);if(focusKey!=null)focusApp(focusKey);if(apps.isEmpty()){TextView settings=findViewById(R.id.btnSettings);settings.requestFocus();}}
 private void appOptions(ResolveInfo r){String key=DashboardApps.key(r);show(new AlertDialog.Builder(this).setTitle(catalog.label(r)).setItems(new String[]{"Move","Hide app","App info"},(d,i)->{
  if(i==0){moving=key;focusKey=key;reload();Toast.makeText(this,"Arrow keys: move · OK/Back: finish",Toast.LENGTH_LONG).show();}
  else if(i==1){AppVisibility.setHidden(this,r.activityInfo.packageName,true);reload();}
  else try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+r.activityInfo.packageName)));}catch(Exception e){message("App","App info is unavailable.");}
 }).setNegativeButton("Close",null));}
 private void open(ResolveInfo r){try{startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){message("App","This app cannot be opened on this device.");}}
 private boolean moveKey(View v,int code,KeyEvent e,String key){
  if(swallowOK&&(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER)){if(e.getAction()==KeyEvent.ACTION_UP)swallowOK=false;return true;}
  if(moving==null)return false;
  if(e.getAction()==KeyEvent.ACTION_UP)return true;
  if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER||code==KeyEvent.KEYCODE_BACK){moving=null;swallowOK=code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER;focusKey=key;reload();return true;}
  if(code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN)return true;
  int delta=code==KeyEvent.KEYCODE_DPAD_LEFT?-1:code==KeyEvent.KEYCODE_DPAD_RIGHT?1:0;
  if(delta!=0){catalog.move(moving,delta);focusKey=moving;reload();return true;}return false;
 }
 private final class Tiles extends RecyclerView.Adapter<Tile> {
  public Tile onCreateViewHolder(ViewGroup parent,int type){LinearLayout card=vertical();card.setGravity(Gravity.CENTER);card.setPadding(d(7),d(4),d(7),d(4));
   RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(d(100),d(80));p.setMargins(d(5),d(2),d(5),d(2));card.setLayoutParams(p);GlassUi.focus(card);card.setBackground(GlassUi.kidsStates(DashboardActivity.this));
   ImageView icon=new ImageView(DashboardActivity.this);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(icon,lp(d(48),d(48)));
   TextView name=text("",12,GlassUi.WHITE);name.setGravity(Gravity.CENTER);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams n=lp(-1,d(20));n.topMargin=d(2);card.addView(name,n);return new Tile(card,icon,name);}
  public void onBindViewHolder(Tile v,int i){ResolveInfo r=apps.get(LoopCarousel.logical(i,apps.size()));String key=DashboardApps.key(r);v.name.setText(catalog.label(r));
   v.icon.setImageDrawable(null);try{v.icon.setImageDrawable(r.loadIcon(getPackageManager()));}catch(Exception ignored){v.icon.setImageDrawable(getPackageManager().getDefaultActivityIcon());}
   v.itemView.setContentDescription(catalog.label(r));v.itemView.setOnFocusChangeListener((x,has)->{if(has){lastFocusedKey=key;((LoopCarousel)grid).effects();}});v.itemView.setOnClickListener(x->open(r));v.itemView.setOnLongClickListener(x->{appOptions(r);return true;});
   v.itemView.setOnKeyListener((x,c,e)->{
    if(moveKey(x,c,e,key))return true;
    if(e.getAction()==KeyEvent.ACTION_DOWN&&(c==KeyEvent.KEYCODE_DPAD_LEFT||c==KeyEvent.KEYCODE_DPAD_RIGHT)){
     if(apps.size()>1){int position=v.getBindingAdapterPosition();if(position==RecyclerView.NO_POSITION)return true;int next=position+(c==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);
      if(next<apps.size()||next>=grid.getAdapter().getItemCount()-apps.size()){next=LoopCarousel.anchor(LoopCarousel.logical(next,apps.size()),apps.size());((LoopCarousel)grid).center(next,false);}else ((LoopCarousel)grid).center(next,true);
     }return true;
    }
    if(e.getAction()==KeyEvent.ACTION_DOWN && (c==KeyEvent.KEYCODE_DPAD_UP||c==KeyEvent.KEYCODE_DPAD_DOWN)){
     if(c==KeyEvent.KEYCODE_DPAD_UP)prayer.requestFocus();return true;
    }
    return false;
   });
   v.itemView.setNextFocusUpId(R.id.prayerPanel);v.itemView.setNextFocusDownId(v.itemView.getId());
   if(key.equals(moving))v.itemView.setBackground(GlassUi.surface(DashboardActivity.this,0xd0234b56,true));else v.itemView.setBackground(GlassUi.kidsStates(DashboardActivity.this));
}

  public int getItemCount(){return LoopCarousel.count(apps.size(),moving!=null);}
 }
 private static final class Tile extends RecyclerView.ViewHolder{final ImageView icon;final TextView name;Tile(View v,ImageView i,TextView n){super(v);icon=i;name=n;}}
 @Override protected void onResume(){super.onResume();resumed=true;wallpaper.hostActive(true);if(hourly!=null)hourly.resume();restoreDim();scheduleWelcome();focusKey=lastFocusedKey!=null?lastFocusedKey:apps.isEmpty()?null:DashboardApps.key(apps.get(0));reload();prayer.start();handler.removeCallbacks(tick);handler.post(tick);rain.resume();}
 @Override protected void onPause(){if(rain!=null)rain.pause();if(hourly!=null)hourly.pause();wallpaper.hostActive(false);resumed=false;handler.removeCallbacks(welcomeTask);welcome.cancel();handler.removeCallbacks(tick);prayer.stop();if(jarvis!=null)jarvis.close();for(Dialog shown:new ArrayList<>(overlays.keySet()))shown.dismiss();super.onPause();}
 @Override protected void onDestroy(){if(rain!=null)rain.destroy();if(hourly!=null)hourly.destroy();if(screenReceiverRegistered){unregisterReceiver(screenReceiver);screenReceiverRegistered=false;}welcome.cancel();handler.removeCallbacksAndMessages(null);prayer.destroy();wallpaper.destroy();super.onDestroy();}
 @Override public void onBackPressed(){if(moving!=null){focusKey=moving;moving=null;reload();}else findViewById(R.id.btnSettings).requestFocus();}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==72)showJarvis();if(request==73){pendingDim=false;if(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED)DimService.enable(this);else message("Dim","Notification permission is required here so the OFF notification remains available. Dim stays OFF.");}}
}
