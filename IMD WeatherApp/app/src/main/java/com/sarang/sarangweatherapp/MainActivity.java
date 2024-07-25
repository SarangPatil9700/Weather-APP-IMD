package com.hrishikesh.omweatherapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.widget.SearchView;

import android.os.Bundle;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.hrishikesh.omweatherapp.databinding.ActivityMainBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    LottieAnimationView lottie;
    ActivityMainBinding binding;
    LocationManager locationManager;
    String current;

    Location loc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        lottie=findViewById(R.id.lottieAnimationView);
        statusCheck();
        getLocation();

        searchCity();

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            },100);

            getLocation();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },101);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            },102);
        }


        Drawable drawable= ResourcesCompat.getDrawable(getResources(),R.drawable.sunny_background,null);
        BitmapDrawable bitmapDrawable= (BitmapDrawable) drawable;
        Bitmap large=bitmapDrawable.getBitmap();
        Notification notification;


        NotificationManager nm= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification=new Notification.Builder(this)
                    .setLargeIcon(large)
                    .setSmallIcon(R.drawable.sea)
                    .setContentText("New MSG")
                    .setSubText("MSG From APP")
                    .setChannelId("MY CHANNEL")
                    .build();

            nm.createNotificationChannel(new NotificationChannel("MY CHANNEL","New Channel", NotificationManager.IMPORTANCE_HIGH));
        }
        else {

            notification=new Notification.Builder(this)
                    .setLargeIcon(large)
                    .setSmallIcon(R.drawable.sea)
                    .setContentText("New MSG")
                    .setSubText("MSG From APP")
                    .build();
        }

        nm.notify(100,notification);


    }

    private void searchCity() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null) {
                    fetchWeatherData(query);
                }
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
    }

    private void fetchWeatherData(String cityName) {
        ApiInterface retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .build().create(ApiInterface.class);
        Call<WeatherApp> response = retrofit.getWeatherData(cityName, "4e3c6bcaa26020acc9703c312f6827e6", "metric");
        response.enqueue(new Callback<WeatherApp>() {
            @Override
            public void onResponse(Call<WeatherApp> call, Response<WeatherApp> response) {
                WeatherApp responseBody = response.body();
                if (response.isSuccessful() && responseBody != null) {
                    String temperature = String.valueOf(responseBody.getMain().getTemp());
                    int humidity = responseBody.getMain().getHumidity();
                    double windSpeed = responseBody.getWind().getSpeed();
                    long sunRise = responseBody.getSys().getSunrise();
                    long sunSet = responseBody.getSys().getSunset();
                    int seaLevel = responseBody.getMain().getPressure();
                    String condition = responseBody.getWeather().size() > 0 ? responseBody.getWeather().get(0).getMain() : "unknown";
                    double maxTemp = responseBody.getMain().getTemp_max();
                    double minTemp = responseBody.getMain().getTemp_min();
                    binding.temp.setText(temperature + " °C");
                    binding.weather.setText(condition);
                    binding.maxTemp.setText("Max Temp: " + maxTemp + " °C");
                    binding.minTemp.setText("Min Temp: " + minTemp + " °C");
                    binding.humidity.setText(humidity + " %");
                    binding.windSpeed.setText(windSpeed + " m/s");
                    binding.sunRise.setText(time(sunRise));
                    binding.sunset.setText(time(sunSet));
                    binding.sea.setText(seaLevel + " hPa");
                    binding.condition.setText(condition);
                    binding.day.setText(dayName(System.currentTimeMillis()));
                    binding.date.setText(date());
                    binding.cityName.setText(cityName);
                    //Log.d("TAG", "onResponse: " + temperature);
                    changeImagesAccordingToWeatherCondition(condition);
                }
            }

            @Override
            public void onFailure(Call<WeatherApp> call, Throwable t) {
                // TODO: Implement what to do on failure
            }
        });
    }


    private void changeImagesAccordingToWeatherCondition(String conditions) {
        switch (conditions) {
            case "Clear Sky":
            case "Sunny":
            case "Clear":
                binding.getRoot().setBackgroundResource(R.drawable.sunny_background);
                lottie.setAnimation(R.raw.sun);
                break;
            case "Partly Clouds":
            case "Clouds":
            case "Overcast":
            case "Mist":
            case "Foggy":
                binding.getRoot().setBackgroundResource(R.drawable.colud_background);
                lottie.setAnimation(R.raw.cloud);
                break;
            case "Light Rain":
            case "Drizzle":
            case "Moderate Rain":
            case "Showers":
            case "Heavy Rain":
                binding.getRoot().setBackgroundResource(R.drawable.rain_background);
                lottie.setAnimation(R.raw.rain);
                break;
            case "Light Snow":
            case "Moderate Snow":
            case "Heavy Snow":
            case "Blizzard":
                binding.getRoot().setBackgroundResource(R.drawable.snow_background);
                lottie.setAnimation(R.raw.snow);
                break;
            default:
                binding.getRoot().setBackgroundResource(R.drawable.sunny_background);
                lottie.setAnimation(R.raw.sun);
                break;
        }
        lottie.playAnimation();
    }




    private String date() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String time(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    public String dayName(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }


    @SuppressLint("MissingPermission")
    private void getLocation() {

        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000,5,MainActivity.this);

            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lastLocation.getLatitude(),lastLocation.getLongitude(),1);
            //   String address = addresses.get(0).getAddressLine(0);
            String city=addresses.get(0).getLocality();
            fetchWeatherData(city);

            Log.d("locc",addresses.get(0).getLocality()+"1");
            Log.d("locc",addresses.get(0).getSubLocality()+"1");
            Log.d("locc",addresses.get(0).getSubAdminArea()+"1");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
       // Toast.makeText(this, ""+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT).show();
        try {

            loc=location;
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
         //   String address = addresses.get(0).getAddressLine(0);
            String city=addresses.get(0).getSubLocality();
            fetchWeatherData(city);
            Log.d("locc",addresses.get(0).getLocality());
            Log.d("locc",addresses.get(0).getSubLocality());
            Log.d("locc",addresses.get(0).getSubAdminArea());
//            Log.d("locc",addresses.get(0).getLocality());
//            Log.d("locc",addresses.get(0).getLocality());
//            Log.d("locc",addresses.get(0).getLocality());

            Log.d("aaaaa",city);


        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        }

    }


    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}