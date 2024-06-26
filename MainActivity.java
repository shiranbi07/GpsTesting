package com.example.samplelocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 123;
    private static final int REQUEST_GPS_SETTINGS = 456;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private TextView textView;
    private List<FreeParkings.FreeParking> freeParkings;

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationPermissionAndFetchLocation();
    }


    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView=findViewById(R.id.locationText);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float speed = ((location.getSpeed()*3600)/1000);
                        textView.setText("Latitude: " + latitude + "\t Longitude: " + longitude + "\t Speed: " + speed + " km/h");
                        try {
                            sendInfoToServer(latitude, longitude, speed);
                            getFreeParking();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        //showToast("Latitude: " + latitude + "\nLongitude: " + longitude);
                    }
                }
            }
        };

        requestLocationPermissionAndFetchLocation();
    }

    private void sendInfoToServer(double latitude, double longitude, float speed) throws JSONException {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "http://34.125.134.54:5001/api/location";
// Request a string response from the provided URL.
        JSONObject postData = new JSONObject();
        String uid = UUID.randomUUID().toString();
        postData.put("id", uid);
        postData.put("latitude", latitude);
        postData.put("longitude", longitude);
        postData.put("speed", speed);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData
                , new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
                        //textView.setText("Response is: " + response);
                    }
                }
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work! error="+ error.toString());
            }
        });



// Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }


    private void getFreeParking() throws JSONException {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "http://34.125.134.54:5001/api/location/free_parking";
// Request a string response from the provided URL.
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray array = response.getJSONArray("items");
                    List<FreeParkings.FreeParking> list = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        list.add(new FreeParkings.FreeParking(array.getJSONObject(i).get("name").toString(), (int)(array.getJSONObject(i).get("count"))));
                    }
                    freeParkings = list;

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                // Display the first 500 characters of the response string.
                textView.setText("Response is: " + freeParkings.size() + " ");
            }
        }
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work! error=" + error.toString());
            }
        });
// Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }


    private void sendInfoToServerWeatherTest(double latitude, double longitude, float speed) {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "http://34.125.134.54:5001/WeatherForecast";
// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        textView.setText("Response is: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work! error="+ error.toString());
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void requestLocationPermissionAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            checkAndFetchLocation();
        }
    }

    private void checkAndFetchLocation() {
        if (isGPSEnabled()) {
            startLocationUpdates();
        } else {
            showToast("GPS is disabled. Please enable it.");
            openGPSSettings();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(500)
                .setFastestInterval(100);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void openGPSSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_GPS_SETTINGS);
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GPS_SETTINGS) {
            checkAndFetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndFetchLocation();
            } else {
                showToast("Please Provide Location Access.");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
