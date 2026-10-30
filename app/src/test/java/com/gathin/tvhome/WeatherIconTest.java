package com.gathin.tvhome;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WeatherIconTest {
    final DashboardTest helper=new DashboardTest();
    final WeatherWidgetTest widgetHelper=new WeatherWidgetTest();
    static final String[] CONDITIONS={"Hujan di beberapa tempat","Ribut petir di kebanyakan tempat","Cerah","Berawan","Hujan renyai","Tidak diketahui"};
    static final String[] NAMES={"rain","thunder","sunny","cloudy","showers","unknown"};
    static final int[] ICONS={R.drawable.ic_weather_rain,R.drawable.ic_weather_thunder,R.drawable.ic_weather_sunny,R.drawable.ic_weather_cloudy,R.drawable.ic_weather_showers,R.drawable.ic_weather_unknown};
    WeatherForecast forecast(String description)throws Exception{
        JSONArray rows=new JSONArray(WeatherWidgetTest.raw());
        for(int i=0;i<rows.length();i++)for(String field:new String[]{"morning_forecast","afternoon_forecast","night_forecast"})rows.getJSONObject(i).put(field,description);
        return WeatherForecast.parse(rows.toString(),"St502",WeatherWidgetTest.NOW.toEpochMilli());
    }
    int selected(WeatherWidget widget)throws Exception{var field=WeatherWidget.class.getDeclaredField("iconResource");field.setAccessible(true);return field.getInt(widget);}
    Bitmap raster(Drawable drawable){Bitmap bitmap=Bitmap.createBitmap(68,68,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,68,68);drawable.draw(new Canvas(bitmap));return bitmap;}
    @Test public void existingForecastStatesAndExplicitLightRainSelectCorrectIcons(){
        for(int i=0;i<CONDITIONS.length;i++)assertEquals(CONDITIONS[i],ICONS[i],WeatherWidget.iconFor(WeatherForecast.classify(CONDITIONS[i]),CONDITIONS[i]));
        assertEquals(R.drawable.ic_weather_unknown,WeatherWidget.iconFor(WeatherForecast.classify("Tiada hujan"),"Tiada hujan"));
        assertEquals(R.drawable.ic_weather_showers,WeatherWidget.iconFor(WeatherForecast.State.UNKNOWN," Hujan  renyai-renyai "));
        assertEquals(R.drawable.ic_weather_sunny,WeatherWidget.iconFor(WeatherForecast.State.UNKNOWN,"Cerah"));
        assertEquals(R.drawable.ic_weather_unknown,WeatherWidget.iconFor(WeatherForecast.State.UNKNOWN,null));
        assertEquals(R.drawable.ic_weather_unknown,WeatherWidget.iconFor(WeatherForecast.State.UNKNOWN,"Hujan tidak dijangka"));
        // The rain-effect/data classifier is intentionally untouched by this icon-only correction.
        assertEquals(WeatherForecast.State.UNKNOWN,WeatherForecast.classify("Hujan renyai"));
    }
    @Test public void actualWidgetFollowsParsedConditionWithoutChangingGeometryOrText()throws Exception{
        helper.demoApps();try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup().visible()){
            View root=helper.render(activity.get(),1920,1080);WeatherWidget widget=root.findViewWithTag("weather-widget");
            Rect original=widgetHelper.bounds(widget);View icon=widget.getChildAt(0);Rect iconBounds=widgetHelper.bounds(icon);
            for(int i=0;i<CONDITIONS.length;i++){
                widget.update(forecast(CONDITIONS[i]),"St502","Sabah",WeatherWidgetTest.NOW,false);helper.render(activity.get(),1920,1080);
                assertEquals(ICONS[i],selected(widget));assertEquals(original,widgetHelper.bounds(widget));assertEquals(iconBounds,widgetHelper.bounds(icon));
                assertEquals(80,icon.getWidth());assertEquals(80,icon.getHeight());
                assertEquals("Sabah",widgetHelper.heading(widget).getText().toString());
                assertEquals("23–34°  "+WeatherWidget.brief(CONDITIONS[i]),widgetHelper.condition(widget).getText().toString());
                widgetHelper.transparent(widget);helper.capture(widget,"weather-icon-"+NAMES[i]);
                if(i==1)helper.capture(root,"weather-icon-corrected-1080");
            }
        }
    }
    @Test public void expiredOrMissingForecastUsesNeutralIconAndUnchangedUpdateDoesNotReload()throws Exception{
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            View root=helper.render(activity.get(),1920,1080);WeatherWidget widget=root.findViewWithTag("weather-widget");
            WeatherForecast forecast=forecast("Hujan");widget.update(forecast,"St502","Sabah",WeatherWidgetTest.NOW,false);helper.render(activity.get(),1920,1080);
            Drawable drawable=((ImageView)widget.getChildAt(0)).getDrawable();widget.update(forecast,"St502","Sabah",WeatherWidgetTest.NOW,false);
            assertSame(drawable,((ImageView)widget.getChildAt(0)).getDrawable());assertFalse(widget.isLayoutRequested());
            widget.update(forecast,"St502","Sabah",WeatherWidgetTest.NOW.plusMillis(WeatherForecast.FRESH_MILLIS),false);assertEquals(R.drawable.ic_weather_unknown,selected(widget));
            widget.update(null,"St502","Sabah",WeatherWidgetTest.NOW,false);assertEquals(R.drawable.ic_weather_unknown,selected(widget));
        }
    }
    @Test public void allNativeVectorsHaveTransparentCornersAndExpectedWeatherMarks()throws Exception{
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            for(int i=0;i<ICONS.length;i++){
                Drawable drawable=activity.get().getDrawable(ICONS[i]);assertTrue(drawable instanceof android.graphics.drawable.VectorDrawable);
                Bitmap pixels=raster(drawable);assertEquals(0,pixels.getPixel(0,0));assertEquals(0,pixels.getPixel(67,0));assertEquals(0,pixels.getPixel(0,67));assertEquals(0,pixels.getPixel(67,67));
                int lowerCyan=0,lowerGold=0;
                for(int y=50;y<68;y++)for(int x=0;x<68;x++){int color=pixels.getPixel(x,y);if(Color.alpha(color)>100&&Color.green(color)>180&&Color.blue(color)>180)lowerCyan++;if(Color.alpha(color)>100&&Color.red(color)>230&&Color.green(color)>130&&Color.blue(color)<150)lowerGold++;}
                if(i==0||i==1||i==4)assertTrue("Visible cyan raindrops below cloud",lowerCyan>15);
                if(i==1)assertTrue("Visible lightning below cloud",lowerGold>15);
                pixels.recycle();
            }
        }
    }
    @Test @org.robolectric.annotation.Config(sdk=26) public void vectorsAlsoLoadOnMinimumSdk(){
        try(var activity=Robolectric.buildActivity(DashboardActivity.class).setup()){
            for(int resource:ICONS){Bitmap bitmap=raster(activity.get().getDrawable(resource));assertEquals(68,bitmap.getWidth());bitmap.recycle();}
        }
    }
}
