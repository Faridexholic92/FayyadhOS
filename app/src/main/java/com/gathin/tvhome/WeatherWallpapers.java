package com.gathin.tvhome;
/** Bundled AI weather artwork. No extra requests; only current-period validated forecast state. */
final class WeatherWallpapers {
 static String asset(WeatherForecast.State state){
  switch(state){
   case CLEAR:return "fayyadh-wallpapers/"+FayyadhWallpapers.DEFAULT;
   case CLOUDY:return "weather-wallpapers/cloudy.jpg";
   case RAIN_EXPECTED:return "weather-wallpapers/rain.jpg";
   case THUNDERSTORM_EXPECTED:return "weather-wallpapers/storm.jpg";
   default:return null;
  }
 }
}
