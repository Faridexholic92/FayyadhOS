package com.gathin.tvhome;
import android.graphics.Typeface;

import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.time.Instant;
import java.util.Locale;

/** Three wallpaper-native text lines. Passive, static, and driven by the existing forecast worker. */
final class WeatherWidget extends LinearLayout {
    private final TextView heading,condition,periods;
    private final ImageView icon;
    private int iconResource;
    WeatherWidget(DashboardActivity a,float scale){
        super(a);setTag("weather-widget");setGravity(Gravity.CENTER_VERTICAL);setOrientation(HORIZONTAL);
        setFocusable(false);setClickable(false);setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        ImageView image=new ImageView(a);icon=image;
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setGravity(Gravity.TOP);
        LayoutParams iconParams=new LayoutParams(a.d(40),a.d(40));iconParams.topMargin=a.d(4);addView(image,iconParams);
        LinearLayout lines=new LinearLayout(a);lines.setOrientation(VERTICAL);
        LayoutParams params=new LayoutParams(0,-2,1);params.leftMargin=a.d(10);addView(lines,params);
        // Dusk layout: place, forecast range + current-period condition, then the next MET periods.
        heading=text(a,10,scale);heading.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        condition=text(a,11,scale);condition.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        periods=text(a,7,scale);periods.setSingleLine(false);periods.setMaxLines(2);periods.setTextColor(0xd9e6eef5);
        lines.addView(heading);lines.addView(condition);lines.addView(periods);
        update(null,"St502","Sabah",Instant.now(),false);
    }
    private TextView text(DashboardActivity a,float size,float scale){
        TextView v=GlassUi.text(a,"",Math.max(5,size*scale),GlassUi.WHITE);
        v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);
        v.setShadowLayer(GlassUi.dp(a,2),0,GlassUi.dp(a,1),0xd9000000);return v;
    }
    private static void changed(TextView view,String value){if(!value.contentEquals(view.getText()))view.setText(value);}
    void update(WeatherForecast forecast,String id,String name,Instant now,boolean cached){
        boolean usable=forecast!=null&&id.equals(forecast.locationId)&&forecast.fresh(now);
        String description=usable?forecast.displayDescription(now):"";
        String range=usable?forecast.temperatureRange(now):"";
        String title=place(name);
        String detail=description.isEmpty()?"Ramalan tidak tersedia":description;
        changed(heading,title);
        CharSequence styled=detail;
        if(!range.isEmpty()){
            android.text.SpannableStringBuilder b=new android.text.SpannableStringBuilder(range.replace("°C","°"));
            b.setSpan(new android.text.style.RelativeSizeSpan(1.75f),0,b.length(),0);
            b.setSpan(new android.text.style.StyleSpan(Typeface.NORMAL),0,b.length(),0);
            b.append("  ").append(brief(detail));styled=b;
        }
        if(!styled.toString().contentEquals(condition.getText()))condition.setText(styled);
        String next=usable?join(forecast.upcoming(now,1),forecast.upcoming(now,2)):"";
        String line=(usable?(forecast.usesDailySummary(now)?"MET harian":"MET"):"MET Malaysia")+(next.isEmpty()?"":" · "+next)+(cached?" · cache":"");
        changed(periods,line);
        String accessibility=title+". "+(range.isEmpty()?"":range+". ")+detail+". "+line+". MET Malaysia / data.gov.my. Bukan pemerhatian semasa.";
        if(RainOptions.HOME_ID.equals(id))accessibility+=" "+RainOptions.HOME_SCOPE;
        if(usable)accessibility+=" Retrieved: "+Instant.ofEpochMilli(forecast.fetchedAt).atZone(WeatherForecast.ZONE)+".";
        if(!accessibility.contentEquals(getContentDescription()==null?"":getContentDescription()))setContentDescription(accessibility);
        int resource=iconFor(usable?forecast.displayState(now):WeatherForecast.State.UNKNOWN,description);
        if(iconResource!=resource){iconResource=resource;icon.setImageResource(resource);}
    }
    static String place(String name){return RainOptions.HOME_NAME.equals(name)?name+", Sabah":name;}
    private static String join(String a,String b){a=brief(a);b=brief(b);return a.isEmpty()?b:b.isEmpty()?a:a+"\n"+b;}
    /** "Ribut petir di beberapa tempat" -> "Ribut petir"; the full text stays in accessibility/details. */
    static String brief(String s){return s.replaceAll(" (?:menyeluruh|di (?:satu dua|beberapa|kebanyakan) tempat.*)$","");}
    /** Icon-only mapping; the forecast text, rain classifier and data source remain unchanged. */
    static int iconFor(WeatherForecast.State state,String description){
        if(state==WeatherForecast.State.THUNDERSTORM_EXPECTED)return R.drawable.ic_weather_thunder;
        if(state==WeatherForecast.State.RAIN_EXPECTED)return R.drawable.ic_weather_rain;
        if(state==WeatherForecast.State.CLOUDY)return R.drawable.ic_weather_cloudy;
        if(state==WeatherForecast.State.CLEAR){
            // "No rain" describes precipitation, not necessarily a clear sky.
            if(description!=null&&description.trim().equalsIgnoreCase("Tiada hujan"))return R.drawable.ic_weather_unknown;
            return R.drawable.ic_weather_sunny;
        }
        // Recognise explicit light rain/sunshine for artwork only, without enabling the rain effect.
        String text=description==null?"":description.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
        if(text.equals("hujan renyai")||text.equals("hujan renyai-renyai"))return R.drawable.ic_weather_showers;
        if(text.equals("cerah"))return R.drawable.ic_weather_sunny;
        return R.drawable.ic_weather_unknown;
    }
}
