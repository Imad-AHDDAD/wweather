package com.example.meteo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private LinearLayout loadingLL, internetErrorLL;
    private Button tryAgainBtn;
    private ProgressBar loadingPB;
    private TextView cityNameTV, loadingTV, temperatureTV, conditionTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private final int PERMISSION_CODE = 1;
    private String cityName;
    private boolean locationReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // initialize elements
        homeRL = findViewById(R.id.idRLHome);
        loadingLL = findViewById(R.id.idLLLoading);
        internetErrorLL = findViewById(R.id.idLLInternetError);
        tryAgainBtn = findViewById(R.id.idTryAgainBtn);
        tryAgainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                internetErrorLL.setVisibility(View.GONE);
                restartApp();
            }
        });

        loadingPB = findViewById(R.id.idPBLoading);
        loadingTV = findViewById(R.id.idTVLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        // hide keyboard and clear focus when click on go
        cityEdt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    // Remove focus from the EditText
                    cityEdt.clearFocus();

                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(cityEdt.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        // clear text when focused
        cityEdt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    cityEdt.setText("");
                }
            }
        });

        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);

        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        // check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if(!isNetworkProviderEnabled){
                // Open the device's settings page for location services
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }else{

                if(isConnectedToInternet()){
                    internetErrorLL.setVisibility(View.GONE);
                    loadingPB.setVisibility(View.VISIBLE);
                    loadingTV.setVisibility(View.VISIBLE);
                    getCurrentLocation();
                }else {
                    internetErrorLL.setVisibility(View.VISIBLE);
                    loadingPB.setVisibility(View.GONE);
                    loadingTV.setVisibility(View.GONE);
                }

            }
        }

        // set up searchBtn
        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city name", Toast.LENGTH_SHORT).show();
                } else {
                    getWeatherInfos(city);
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                if(isConnectedToInternet()){
                    internetErrorLL.setVisibility(View.GONE);
                    loadingPB.setVisibility(View.VISIBLE);
                    loadingTV.setVisibility(View.VISIBLE);
                    getCurrentLocation();
                }else {
                    internetErrorLL.setVisibility(View.VISIBLE);
                    loadingPB.setVisibility(View.GONE);
                    loadingTV.setVisibility(View.GONE);
                }
            } else {
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // la methode getCityName from latitude and longitude
    private String getCityName(double latitude, double longitude) {
        String cityName = "Not found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);
            for (Address adr : addresses) {
                if (adr != null) {
                    Log.d("ADR_FOUND", "adr = " + adr);
                    String city = adr.getLocality();
                    Log.d("CITY_FOUND", "city = " + city);
                    if (city != null && !city.equals("") && (isEnglishWord(city) || isFrenchWord(city))) {
                        cityName = city;
                        getWeatherInfos(cityName);
                    } else {
                        Log.d("CITY_NOT_FOUND", "CITY NOT FOUND");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    // la methode getWeatherInfos
    private void getWeatherInfos(String cityName) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=d0c0bfe25efe4db498c145330231506&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                cityNameTV.setText(cityName);
                loadingLL.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + " °C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);

                    if (isDay == 1) {
                        // morning
                        Picasso.get().load("https://w0.peakpx.com/wallpaper/370/935/HD-wallpaper-up-cloud-view-beautiful-clouds-good-love-morning-phone-pink-popular-romantic.jpg").into(backIV);
                    } else {

                        Picasso.get().load("https://w0.peakpx.com/wallpaper/261/521/HD-wallpaper-sunset-dawn-evening-good-orange-graphy-sun-sunlight-weather.jpg").into(backIV);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastO.getJSONArray("hour");

                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind));
                    }

                    weatherRVAdapter.notifyDataSetChanged();
                    locationReceived = true;

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(isConnectedToInternet()){
                    Toast.makeText(MainActivity.this, "Please a valid city name " + cityName, Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                }
                Log.d("INVALID_CITY", "error: " + error.toString());
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null && !locationReceived) {
                        cityName = getCityName(location.getLatitude(), location.getLongitude());
                    }
                }

            });
        }
    }

    private Location getLastKnownLocation() {
        Log.d("PROVIDER_LOCATION", "in getLastKnownLocation method");
        List<String> providers = locationManager.getProviders(true);
        Log.d("PROVIDER_LOCATION", "providers list size : "+providers.size());
        Location bestLocation = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show();
        }else {
            for (String provider : providers) {
                Log.d("PROVIDER_LOCATION", "provider : "+provider);
                Location l = locationManager.getLastKnownLocation(provider);
                if (l == null) {
                    Log.d("PROVIDER_LOCATION", "location using provider "+provider+" is null");
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            }
        }
        return bestLocation;
    }

    private boolean isFrenchWord(String word) {
        for (char c : word.toCharArray()) {
            if (!isFrenchLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isEnglishWord(String word) {
        for (char c : word.toCharArray()) {
            if (!isEnglishLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFrenchLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= 'À' && c <= 'ÿ');
    }

    private boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
    private void restartApp() {
        Context context = getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}