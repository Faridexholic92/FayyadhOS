package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Remote-friendly prayer strip. Work stops when Home is hidden; zone must be chosen explicitly. */
final class PrayerPanel extends LinearLayout {
    private static final int WHITE=0xfff4f7fb, MUTED=0xffedf3f7, GOLD=0xffb9f5d8;
    private final Activity activity;
    private final SharedPreferences prefs;
    private final PrayerRepository repository;
    private final Clock clock;
    private int glowingIndex=-1;
    private android.animation.ValueAnimator glowAnimator;
    private boolean obscured;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Map<LocalDate,PrayerTimes.Day> days=new TreeMap<>();
    private final List<String[]> zones=new ArrayList<>();
    private final TextView heading, subtitle;
    private TextView azanStatus;
    private final TextView[] labels=new TextView[6], values=new TextView[6];
    private String zone="", lastDate="", failure="";
    private boolean active, busy, online, dashboard;
    private float dashboardScale=1f;
    private long retryAt;
    private int generation;
    private AlertDialog dialog;
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(!active) return;
            render(); refresh(false);
            Instant now=clock.instant();
            handler.postDelayed(this,PrayerTimes.nextTickMillis(days.get(now.atZone(PrayerTimes.MALAYSIA).toLocalDate()),now));
        }
    };
    PrayerPanel(Activity owner) { this(owner,Clock.systemUTC()); }
    PrayerPanel(Activity owner,Clock clock) {
        super(owner); activity=owner;this.clock=clock;
        prefs=owner.getSharedPreferences("solat",Activity.MODE_PRIVATE);
        repository=new PrayerRepository(owner,clock);
        setId(R.id.prayerPanel); setOrientation(VERTICAL);
        setPadding(dp(10),dp(4),dp(10),dp(4));
        setFocusable(true); setClickable(true);
        setContentDescription("JAKIM prayer times. Press OK for zone and options. Hold OK for FayyadhOS wallpapers.");
        setNextFocusDownId(R.id.grid); setNextFocusUpId(R.id.btnSettings);
        setBackground(panelBackground(false));
        setOnFocusChangeListener((v,focus)->{setBackground(panelBackground(focus));invalidate();});
        setOnClickListener(v->showOptions());
        setOnLongClickListener(v->{
            if(activity instanceof DashboardActivity) { ((DashboardActivity)activity).wallpapers(); return true; }
            if(activity instanceof MainActivity) { ((MainActivity)activity).showFayyadhWallpaperPicker(); return true; }
            return false;
        });
        LinearLayout header=new LinearLayout(owner); header.setGravity(Gravity.CENTER_VERTICAL);
        heading=text(10,WHITE); heading.setSingleLine(true);
        heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        header.addView(heading,new LayoutParams(0,dp(16),1));
        TextView source=text(9,MUTED); source.setText("e-Solat JAKIM");
        header.addView(source,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(16)));
        addView(header);
        LinearLayout row=new LinearLayout(owner); row.setPadding(0,dp(2),0,dp(2));
        for(int i=0;i<6;i++) {
            LinearLayout cell=new LinearLayout(owner); cell.setOrientation(VERTICAL);
            labels[i]=text(10,MUTED); labels[i].setText(PrayerTimes.NAMES[i]);
            values[i]=text(17,WHITE); values[i].setTypeface(null,Typeface.BOLD);
            cell.addView(labels[i]); cell.addView(values[i]);
            row.addView(cell,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
        }
        addView(row);
        subtitle=text(9,MUTED); subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END); addView(subtitle);
        try(InputStream in=owner.getAssets().open("jakim-zones.json")) {
            byte[] raw=new byte[in.available()]; new java.io.DataInputStream(in).readFully(raw);
            JSONArray list=new JSONArray(new String(raw,java.nio.charset.StandardCharsets.UTF_8));
            for(int i=0;i<list.length();i++) {
                JSONObject z=list.getJSONObject(i);
                zones.add(new String[]{z.getString("code"),z.getString("state"),z.getString("name")});
            }
        } catch(Exception e) { android.util.Log.e("Solat","Zone catalogue unavailable",e); }
        String saved=prefs.getString("zone","");
        for(String[] z:zones) if(z[0].equals(saved)) zone=saved;
        render();
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(int size,int color) {
        TextView t=new OutlinedTextView(activity); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false);
        t.setFontFeatureSettings("tnum");
        t.setShadowLayer(dp(2),0,dp(1),0xe6000000);
        t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); return t;
    }
    /** Thin glyph outline, not a rectangular background: readable on bright wallpapers too. */
    private final class OutlinedTextView extends TextView {
        OutlinedTextView(android.content.Context context) { super(context); }
        @Override protected void onDraw(android.graphics.Canvas canvas) {
            android.text.Layout layout=getLayout();
            if(layout!=null) {
                android.text.TextPaint paint=getPaint();
                int color=paint.getColor();
                android.graphics.Paint.Style style=paint.getStyle();
                float stroke=paint.getStrokeWidth();
                android.graphics.Paint.Join join=paint.getStrokeJoin();
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.45f*getResources().getDisplayMetrics().density);
                paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                paint.setColor(0xe6000000);
                int saved=canvas.save();
                canvas.translate(getCompoundPaddingLeft(),getTotalPaddingTop());
                layout.draw(canvas);
                canvas.restoreToCount(saved);
                paint.setColor(color);paint.setStyle(style);paint.setStrokeWidth(stroke);paint.setStrokeJoin(join);
            }
            super.onDraw(canvas);
        }
    }
    private GradientDrawable panelBackground(boolean focused) {
        GradientDrawable b=new GradientDrawable(); b.setColor(Color.TRANSPARENT);
        b.setCornerRadius(dp(8)); b.setStroke(dp(!dashboard&&focused?2:0),!dashboard&&focused?GlassUi.CYAN:Color.TRANSPARENT); return b;
    }
    /** Five simultaneous fardu times below the brand; no card, Syuruk or rotating slot. */
    void dashboardScale(float scale) {
        dashboard=true;dashboardScale=scale;setFocusableInTouchMode(true);
        for(TextView label:labels) if(label.getParent()!=null)((ViewGroup)label.getParent()).removeView(label);
        for(TextView value:values) if(value.getParent()!=null)((ViewGroup)value.getParent()).removeView(value);
        if(heading.getParent()!=null)((ViewGroup)heading.getParent()).removeView(heading);
        if(subtitle.getParent()!=null)((ViewGroup)subtitle.getParent()).removeView(subtitle);
        removeAllViews();setPadding(0,scaled(1),0,0);setClipChildren(false);setClipToPadding(false);
        LinearLayout premiumHeader=new LinearLayout(activity);premiumHeader.setGravity(Gravity.CENTER_VERTICAL);
        heading.setTextSize(13*scale);heading.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));heading.setTag("next-prayer");
        premiumHeader.addView(heading,new LayoutParams(0,scaled(27),1));
        azanStatus=text(8,GlassUi.MUTED);azanStatus.setTextSize(8*scale);azanStatus.setTag("azan-status");premiumHeader.addView(azanStatus,new LayoutParams(scaled(78),scaled(18)));
        addView(premiumHeader,new LayoutParams(-1,scaled(29)));
        LinearLayout row=new LinearLayout(activity);row.setClipChildren(false);row.setClipToPadding(false);row.setTag("fivePrayerRow");
        for(int index:PrayerTimes.PRAYERS){
            LinearLayout cell=new LinearLayout(activity);cell.setOrientation(VERTICAL);cell.setClipChildren(false);cell.setClipToPadding(false);
            labels[index].setTextSize(9*scale);labels[index].setSingleLine(true);labels[index].setTag("prayerName-"+index);
            values[index].setTextSize(12*scale);values[index].setSingleLine(true);values[index].setTag("prayerTime-"+index);
            cell.addView(labels[index],new LayoutParams(-1,scaled(14)));
            cell.addView(values[index],new LayoutParams(-1,scaled(19)));
            row.addView(cell,new LayoutParams(0,-2,1));
        }
        addView(row,new LayoutParams(-1,-2));subtitle.setTextSize(6*scale);subtitle.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));subtitle.setTag("prayer-provenance");addView(subtitle,new LayoutParams(-1,scaled(16)));setBackground(panelBackground(hasFocus()));render();
    }
    void obscured(boolean value){obscured=value;syncGlow();}
    // All five remain visible. Left/right must never rotate or replace the current slot.
    boolean cycle(int direction){return false;}
    private void cancelGlow(){if(glowAnimator!=null){glowAnimator.cancel();glowAnimator=null;}}
    private void staticGlow(){
        if(glowingIndex>=0)labels[glowingIndex].setShadowLayer(scaled(4),0,0,0x9967dce8);
    }
    private void syncGlow(){
        if(!dashboard)return;
        boolean animate=active&&!obscured&&isAttachedToWindow()&&isShown()&&getWindowVisibility()==VISIBLE&&glowingIndex>=0&&android.animation.ValueAnimator.areAnimatorsEnabled()&&android.provider.Settings.Global.getFloat(activity.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;
        if(!animate){cancelGlow();staticGlow();return;}
        if(glowAnimator!=null&&glowAnimator.isRunning())return;
        final TextView name=labels[glowingIndex];
        glowAnimator=android.animation.ValueAnimator.ofFloat(0f,1f);glowAnimator.setDuration(1800);glowAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);glowAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        glowAnimator.addUpdateListener(a->{float v=(float)a.getAnimatedValue();int alpha=Math.round(90+100*v);name.setShadowLayer(scaled(3+4*v),0,0,Color.argb(alpha,103,220,232));});
        glowAnimator.start();
    }
    private void updateGlow(int index){
        if(index!=glowingIndex){cancelGlow();glowingIndex=index;}
        for(int i:PrayerTimes.PRAYERS){
            labels[i].setTextColor(i==glowingIndex?GlassUi.CYAN:GlassUi.WHITE);
            if(i!=glowingIndex)labels[i].setShadowLayer(dp(2),0,dp(1),0xe6000000);
            values[i].setTextColor(GlassUi.WHITE); // Only the current NAME glows, never the time or widget.
        }
        syncGlow();
    }
    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);if(dashboard)syncGlow();}
    @Override protected void onDetachedFromWindow(){cancelGlow();super.onDetachedFromWindow();}
    @Override protected void dispatchDraw(android.graphics.Canvas canvas){
        super.dispatchDraw(canvas);
        // A focus underline, not a filled card or rectangle; remote OK still opens all settings.
        if(dashboard&&hasFocus()){android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(GlassUi.CYAN);p.setStrokeWidth(Math.max(1,scaled(1)));canvas.drawLine(0,getHeight()-scaled(1),getWidth(),getHeight()-scaled(1),p);}
    }
    private int scaled(float n){return dp(n*dashboardScale);}
    private String regionName(){for(String[] z:zones)if(z[0].equals(zone))return z[1].toUpperCase(Locale.getDefault());return "SELECT ZONE";}
    void playbackChanged(){render();}
    void modeChanged() { render(); if(active) refresh(false); }
    void start() {
        if(active) return; active=true; loadCache(); handler.removeCallbacks(tick); handler.post(tick); syncGlow();AzanScheduler.reconcile(activity);
    }
    void stop() {
        active=false; handler.removeCallbacks(tick);cancelGlow();
        if(dialog!=null) dialog.dismiss();
    }
    void destroy() { stop(); generation++; worker.shutdownNow(); }
    private void loadCache() {
        days.clear(); online=false;
        if(zone.isEmpty()) return;
        YearMonth month=YearMonth.from(clock.instant().atZone(PrayerTimes.MALAYSIA));
        // Tiny local files (< 12 KB/month); no network on UI thread.
        days.putAll(repository.cached(zone,month));
        days.putAll(repository.cached(zone,month.plusMonths(1)));
    }
    private void refresh(boolean force) {
        if(!active || busy || zone.isEmpty()) return;
        if(!force && !NetworkFocus.automaticPrayer(activity)) return;
        Instant instant=clock.instant();
        String date=instant.atZone(PrayerTimes.MALAYSIA).toLocalDate().toString();
        if(!force && date.equals(lastDate) && android.os.SystemClock.elapsedRealtime()<retryAt) return;
        lastDate=date; busy=true;
        final int request=++generation; final String requestedZone=zone; final long requestStarted=clock.millis();
        retryAt=android.os.SystemClock.elapsedRealtime()+15*60*1000L;
        worker.execute(()->{
            Map<LocalDate,PrayerTimes.Day> result=new TreeMap<>();
            boolean success=false; String problem="";
            try {
                LocalDate today=clock.instant().atZone(PrayerTimes.MALAYSIA).toLocalDate();
                YearMonth current=YearMonth.from(today);
                result.putAll(repository.fetch(requestedZone,current,force));
                if(today.getDayOfMonth()>=28) result.putAll(repository.fetch(requestedZone,current.plusMonths(1),force));
                if(!result.containsKey(today)) throw new IllegalStateException("Today's schedule missing");
                success=true;
            } catch(Exception e) {
                problem=e instanceof java.net.SocketTimeoutException?"API request timed out":(e instanceof IllegalArgumentException||e instanceof org.json.JSONException)?"Invalid API data":"API unreachable";
                android.util.Log.w("Solat","JAKIM e-Solat unavailable; using validated cache",e);
            }
            final boolean ok=success; final String error=problem;
            handler.post(()->{
                if(request!=generation) return;
                busy=false; failure=error; online=ok && repository.updatedAt(requestedZone,YearMonth.from(clock.instant().atZone(PrayerTimes.MALAYSIA)))>=requestStarted;
                if(!requestedZone.equals(zone)) return;
                days.putAll(result);
                if(ok)AzanScheduler.reconcile(activity);
                if(ok) retryAt=android.os.SystemClock.elapsedRealtime()+24*60*60*1000L;
                if(active) render();
            });
        });
        render();
    }
    private String zoneName() {
        for(String[] z:zones) if(z[0].equals(zone)) return z[0]+" · "+z[2];
        return zone;
    }
    private void render() {
        Instant now=clock.instant(); LocalDate today=now.atZone(PrayerTimes.MALAYSIA).toLocalDate();
        PrayerTimes.Day day=days.get(today);
        heading.setText(dashboard?regionName()+(zone.isEmpty()?"":" · "+zone):zone.isEmpty()?"PRAYER TIMES · Select your JAKIM zone":zoneName());
        PrayerTimes.Next next=PrayerTimes.next(days,now);
        for(int i=0;i<6;i++) {
            values[i].setText(day==null?"--:--":PrayerTimes.hhmm(day.times[i]));
            boolean selected=i==PrayerTimes.current(day,now);
            if(!dashboard){values[i].setTextColor(selected?GOLD:WHITE); labels[i].setTextColor(selected?GOLD:MUTED);}
        }
        if(dashboard){
            String nextLabel=next==null?(zone.isEmpty()?"NEXT PRAYER · Select zone":"NEXT PRAYER · Unavailable"):
                "NEXT · "+PrayerTimes.NAMES[next.index]+(next.tomorrow?" tomorrow":"")+"  "+PrayerTimes.hhmm(next.at.toLocalTime())+"  ·  "+next.minutesUntil(now)+" min";
            heading.setText(nextLabel);
            if(azanStatus!=null){boolean enabled=AzanStore.enabled(activity);
                boolean ready=enabled&&AzanScheduler.exactAllowed(activity)&&AzanScheduler.notificationsAllowed(activity)&&AzanStore.present(AzanStore.audio(activity,next==null?2:next.index))&&AzanStore.prefs(activity).getLong("pending_at",0)>System.currentTimeMillis();
                azanStatus.setText(AzanService.playing()?"AZAN PLAYING":!enabled?"AZAN OFF":ready?"AZAN ON":"AZAN · CHECK");}
            int current=PrayerTimes.current(day,now);updateGlow(current);
            StringBuilder description=new StringBuilder(zone.isEmpty()?"Pilih zon JAKIM. ":zoneName()+". ");
            for(int index:PrayerTimes.PRAYERS)description.append(PrayerTimes.NAMES[index]).append(" ").append(values[index].getText()).append(current==index?" semasa. ":". ");
            description.append("OK: jadual lengkap dan tetapan. Tekan lama: wallpaper.");setContentDescription(description.toString());
        }
        if(zone.isEmpty()) { subtitle.setText("Press OK to select your state and zone. No location is selected automatically."); return; }
        if(day==null) {
            if(!NetworkFocus.automaticPrayer(activity) && !busy) { subtitle.setText("Focus mode · No schedule for today. Refresh from prayer options."); return; }
            subtitle.setText(busy?"Downloading prayer schedule…":"Today's schedule is unavailable. "+(failure.isEmpty()?"Check your Internet connection and TV date.":failure)+" Select Refresh to try again.");
            return;
        }
        String date=today.format(DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ENGLISH));
        long stamp=repository.updatedAt(zone,YearMonth.from(today));
        String updated=stamp<=0?"":Instant.ofEpochMilli(stamp).atZone(PrayerTimes.MALAYSIA).format(DateTimeFormatter.ofPattern("d MMM h:mm a",Locale.ENGLISH));
        String status=busy?"Updating…":online?"JAKIM e-Solat":"JAKIM cache";
        if(!updated.isEmpty()) status+=" · "+updated;
        if(!failure.isEmpty()&&!busy) status+=" · "+failure;
        String line=(dashboard?zoneName()+" · ":"")+date+" · "+day.hijri+" H · ";
        if(next!=null) {
            long minutes=next.minutesUntil(now);
            line+=PrayerTimes.NAMES[next.index]+(next.tomorrow?" tomorrow":"")+" "+PrayerTimes.hhmm(next.at.toLocalTime())+
                    " ("+(minutes/60)+"h "+(minutes%60)+"m) · ";
        } else line+="Tomorrow's Fajr schedule is unavailable · ";
        if(dashboard){String source=busy?"Memuat…":online?"e-Solat JAKIM":"Cache JAKIM";subtitle.setText(zone+" · "+activity.getString(R.string.solat_source_stamp,source,updated.isEmpty()?"—":updated,failure.isEmpty()?"":" · gagal kemas kini"));return;}
        subtitle.setText(line+status+(!NetworkFocus.automaticPrayer(activity)?" · Focus: manual refresh":"")+" · MYT (UTC+8)");
    }
    private void showSchedule(){
        Instant now=clock.instant();PrayerTimes.Day day=days.get(now.atZone(PrayerTimes.MALAYSIA).toLocalDate());
        StringBuilder message=new StringBuilder(zoneName()+"\n"+now.atZone(PrayerTimes.MALAYSIA).toLocalDate()+" · MYT\n\n");
        for(int i=0;i<6;i++)message.append(PrayerTimes.NAMES[i]).append(i==1?" (bukan solat fardu)":"").append("   ").append(day==null?"--:--":PrayerTimes.hhmm(day.times[i])).append("\n");
        message.append("\n").append(subtitle.getText());
        dialog=present(new AlertDialog.Builder(activity).setTitle("Jadual e-Solat JAKIM").setMessage(message.toString()).setPositiveButton("Tutup",null));
    }
    private AlertDialog present(AlertDialog.Builder builder) {
        if(activity instanceof DashboardActivity) return ((DashboardActivity)activity).show(builder,true);
        return builder.show();
    }
    private void showOptions() {
        if(zone.isEmpty()) { chooseState(); return; }
        dialog=present(new AlertDialog.Builder(activity).setTitle("Prayer times · "+zone)
                .setItems(new String[]{"Change state / JAKIM zone","Refresh prayer times","FayyadhOS wallpapers","Source & information","Jadual lengkap · 6 waktu","Azan · audio & tetapan"},(d,which)->{
                    if(which==0) chooseState();
                    else if(which==1) refresh(true);
                    else if(which==2 && activity instanceof DashboardActivity) ((DashboardActivity)activity).wallpapers();
                    else if(which==2 && activity instanceof MainActivity) ((MainActivity)activity).showFayyadhWallpaperPicker();
                    else if(which==4)showSchedule();
                    else if(which==5)activity.startActivity(new android.content.Intent(activity,AzanSettingsActivity.class));
                    else dialog=present(new AlertDialog.Builder(activity).setTitle("Prayer time source")
                            .setMessage("e-Solat JAKIM rasmi\nhttps://www.e-solat.gov.my/\n\n"+
                                    "Negeri dan bandar dipadankan kepada zon JAKIM yang anda pilih. Tiada zon dipilih secara automatik. " +
                                    "Jadual bulanan yang disahkan disimpan pada TV; hanya tarikh hari ini dipaparkan. Data cache ditanda, bukan dianggap muat turun baharu. " +
                                    "Muat semula pada tengah malam Malaysia semasa Home aktif; jika TV tidur, semak semula apabila Home dibuka. Tiada servis latar belakang.\n\n"+
                                    "Waktu semasa mengikut sela jadual; Subuh berhenti diserlah selepas Syuruk. Sebelum Subuh tiada nama diserlah. Ini bukan fatwa had waktu solat. " +
                                    "Syuruk bukan solat fardu. Lima waktu dipaparkan serentak di Home; hanya nama waktu semasa bercahaya lembut. Animasi dijeda semasa dialog/TV tidur dan menghormati tetapan animasi sistem.\n\n"+
                                    "Aplikasi komuniti, bukan aplikasi rasmi JAKIM. Zon dihantar terus ke www.e-solat.gov.my. Tiada GPS, akaun atau kunci API diperlukan.")
                            .setPositiveButton("Close",null) );
                }).setNegativeButton("Close",null) );
    }
    private void chooseState() {
        LinkedHashSet<String> states=new LinkedHashSet<>(); for(String[] z:zones) states.add(z[1]);
        String[] names=states.toArray(new String[0]);
        dialog=present(new AlertDialog.Builder(activity).setTitle("Select your state")
                .setItems(names,(d,i)->chooseZone(names[i])).setNegativeButton("Cancel",null) );
    }
    private void chooseZone(String state) {
        List<String[]> matching=new ArrayList<>(); for(String[] z:zones) if(z[1].equals(state)) matching.add(z);
        String[] names=new String[matching.size()]; int selected=-1;
        for(int i=0;i<names.length;i++) {
            String[] z=matching.get(i); names[i]=z[0]+" — "+z[2]; if(z[0].equals(zone)) selected=i;
        }
        dialog=present(new AlertDialog.Builder(activity).setTitle("JAKIM zone · "+state)
                .setSingleChoiceItems(names,selected,(d,i)->{
                    d.dismiss(); zone=matching.get(i)[0]; generation++; busy=false;
                    prefs.edit().putString("zone",zone).apply();
                    retryAt=0; lastDate=""; loadCache(); render();AzanScheduler.reconcile(activity);refresh(true);
                }).setNegativeButton("Back",(d,i)->chooseState()) );
    }
}
