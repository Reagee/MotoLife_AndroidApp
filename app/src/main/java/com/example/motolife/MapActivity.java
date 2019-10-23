package com.example.motolife;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static com.example.motolife.R.string.RIDING_WAKE_LOCK;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationClickListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String API_URL = "http://192.168.0.16:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_map);

        PowerManager manager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock =
                Objects.requireNonNull(manager).newWakeLock(PARTIAL_WAKE_LOCK, getString(RIDING_WAKE_LOCK));

        checkUsername();
        if (checkLocalizationPersmissions())
            initializeMap();
    }

    private boolean checkLocalizationPersmissions() {
        while (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1
            );
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Permission granted !", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Permissions have to be granted to continue.", Toast.LENGTH_LONG).show();
                finish();
                System.exit(0);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(300);
        locationRequest.setFastestInterval(300);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        setMapSettings();
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationsList = locationResult.getLocations();

            if (locationsList.size() > 0) {
                Location location = locationsList.get(locationsList.size() - 1);
                LatLng nextLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(nextLatLng)
                        .zoom(18)
                        .tilt(45)
                        .build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                try {
                    new UserLocalizationUpdate().execute(location.getLatitude(), location.getLongitude()).get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void setMapSettings() {
        mMap.setIndoorEnabled(false);
        mMap.setMapType(MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.setOnMyLocationClickListener(this);
    }

    @Override
    public void onBackPressed() {
    }


    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(MapActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();
        return true;
    }

    public void initializeMap() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocalizationPersmissions();

        try {
            String result = new UserLocationGetter().execute().get();
            Toast.makeText(getApplicationContext(), "Results: "+result, Toast.LENGTH_LONG).show();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void checkUsername() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains("username")) {
            AlertDialog.Builder usernameDialog = new AlertDialog.Builder(this);
            usernameDialog
                    .setTitle("Enter your username")
                    .setMessage("It is required to show your location on a map.");

            final EditText usernameInput = new EditText(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.MarginLayoutParams.WRAP_CONTENT);
            usernameInput.setLayoutParams(lp);
            usernameDialog.setView(usernameInput);
            usernameDialog
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = preferences.edit();
                            String username = usernameInput.getText().toString();
                            if (!Objects.equals(username, null) && !username.isEmpty()) {
                                editor.putString("username", usernameInput.getText().toString());
                                editor.apply();
                                dialog.dismiss();
                            } else {
                                usernameInput.setError("Provide your username");
                            }
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onMyLocationClick(Location location) {
        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentLocation)
                .zoom(18)
                .bearing(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private class UserLocationGetter extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            final String[] result = {null};
            StringRequest request = new StringRequest
                    (Request.Method.GET, API_URL + "getLocations", response -> {
                        System.out.println(response);
                        result[0] = response;
                    },
                            error -> {
                                Toast.makeText(getApplicationContext(), "Cannot update users' location.", Toast.LENGTH_LONG).show();
                            });
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(request);
            return result[0];
        }
    }

    private void refreshUsersLocation() throws ExecutionException, InterruptedException {
        String result = new UserLocationGetter().execute().get();
        Toast.makeText(getApplicationContext(), "Results: "+result, Toast.LENGTH_LONG).show();
    }

    private class UserLocalizationUpdate extends AsyncTask<Double, Void, Void> {

        @Override
        protected Void doInBackground(Double... userLocations) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (preferences.contains("username")) {
                StringRequest request = new StringRequest
                        (Request.Method.GET, API_URL + "updateUserLocation" +
                                "?username=" + preferences.getString("username",
                                "user" + new Random().nextInt(10000)) +
                                "&latitude=" + userLocations[0] +
                                "&longitude=" + userLocations[1], response ->
                                Toast.makeText(getApplicationContext(), "Location's update has been sent.", Toast.LENGTH_LONG).show(),
                                error -> {
                                    Toast.makeText(getApplicationContext(), "Cannot update user location.", Toast.LENGTH_LONG).show();
                                });
                System.out.println("!!!!!!!!!!!! " + request.toString() + " !!!!!!!!!!!!");
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
                requestQueue.add(request);
            }
            return null;
        }
    }
}
