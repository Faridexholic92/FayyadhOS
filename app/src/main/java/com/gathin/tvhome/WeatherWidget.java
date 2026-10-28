package com.gathin.tvhome;
import android.graphics.Typeface;

import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.time.Instant;
import java.util.Locale;

/** Two wallpaper-native text lines. Passive, static, and driven by the existing forecast worker. */
final class WeatherWidget extends LinearLayout {
    private final TextView heading,condition;
    private final ImageView icon;
    private int iconResource;
    WeatherWidget(DashboardActivity a,float scale){
        super(a);setTag("weather-widget");setGravity(Gravity.CENTER_VERTICAL);setOrientation(HORIZONTAL);
        setFocusable(false);setClickable(false);setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        ImageView image=new ImageView(a);icon=image;
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(image,new LayoutParams(a.d(34),a.d(34)));
        LinearLayout lines=new LinearLayout(a);lines.setOrientation(VERTICAL);
        LayoutParams params=new LayoutParams(0,-2,1);params.leftMargin=a.d(9);addView(lines,params);
        heading=text(a,16,scale);heading.setTypeface(null,Typeface.BOLD);
        condition=text(a,12,scale);lines.addView(heading);lines.addView(condition);
        update(null,"St502","Sabah",Instant.now(),false);
    }
    private TextView text(DashboardActivity a,float size,float scale){
        TextView v=GlassUi.text(a,"",Math.max(8,size*scale),GlassUi.WHITE);
        v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);
        v.setShadowLayer(GlassUi.dp(a,2),0,GlassUi.dp(a,1),0xd9000000);return v;
    }
    private static void changed(TextView view,String value){if(!value.contentEquals(view.getText()))view.setText(value);}
    void update(WeatherForecast forecast,String id,String name,Instant now,boolean cached){
        boolean usable=forecast!=null&&id.equals(forecast.locationId)&&forecast.fresh(now);
        String description=usable?forecast.displayDescription(now):"";
        String range=usable?forecast.temperatureRange(now):"";
        String title=name+(range.isEmpty()?"":" · "+range);
        String detail=description.isEmpty()?"Ramalan tidak tersedia":forecast.displayLabel(now)+" · "+description+(cached?" · cache":"");
        changed(heading,title);changed(condition,detail);
        String accessibility=title+". "+detail+". MET Malaysia / data.gov.my. Bukan pemerhatian semasa.";
        if(RainOptions.HOME_ID.equals(id))accessibility+=" "+RainOptions.HOME_SCOPE;
        if(usable)accessibility+=" Retrieved: "+Instant.ofEpochMilli(forecast.fetchedAt).atZone(WeatherForecast.ZONE)+".";
        if(!accessibility.contentEquals(getContentDescription()==null?"":getContentDescription()))setContentDescription(accessibility);
        int resource=iconFor(usable?forecast.displayState(now):WeatherForecast.State.UNKNOWN,description);
        if(iconResource!=resource){iconResource=resource;icon.setImageResource(resource);}
    }
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
