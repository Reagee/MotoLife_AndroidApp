package com.example.motolife;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.motolife.ui.model.UserLocation;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static com.example.motolife.R.string.RIDING_WAKE_LOCK;
import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, HttpCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private RequestQueue requestQueue;
    private JSONArray usersLocation;
    private Switch darkModeSwitch;
    private BottomNavigationView bottomBar;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SharedPreferences sharedPreferences;
    private static final String API_URL = "http://s1.ct8.pl:25500/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_map);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        PowerManager manager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock =
                Objects.requireNonNull(manager).newWakeLock(PARTIAL_WAKE_LOCK, getString(RIDING_WAKE_LOCK));
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(4000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

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

                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                        .zoom(mMap.getCameraPosition().zoom)
                        .bearing(mMap.getCameraPosition().bearing)
                        .tilt(mMap.getCameraPosition().tilt)
                        .target(mMap.getCameraPosition().target)
                        .build()));
                updateUserLocation(location.getLatitude(), location.getLongitude());
                try {
                    refreshUsersLocationOnMap();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void setMapSettings() {
        mMap.setIndoorEnabled(false);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.standard_map));
        mMap.setMapType(MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabledDuringRotateOrZoom(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMyLocationClickListener(this);
        mMap.setOnMarkerClickListener(this);

        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map));
                darkModeSwitch.setTextColor(Color.WHITE);
            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.standard_map));
                darkModeSwitch.setTextColor(Color.BLACK);
            }
        });
    }


    @Override
    public void onBackPressed() {
    }


    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
        Task<Location> startLocation = LocationServices.getFusedLocationProviderClient(getApplicationContext()).getLastLocation();
        startLocation.addOnSuccessListener(listener -> {
            if (listener != null) {
                LatLng loc = new LatLng(
                        startLocation.getResult().getLatitude(),
                        startLocation.getResult().getLongitude());
                mMap.moveCamera(newLatLngZoom(loc, 12.0f));
            }
        });
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    public void initializeMap() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        bottomBar = findViewById(R.id.map_bottom_nav);
        bottomBar.setOnNavigationItemSelectedListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.nav_message:
                    Toast.makeText(getApplicationContext(),"User messaged !",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.nav_poke:
                    Toast.makeText(getApplicationContext(),"User Poked !",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.nav_exit:
                    bottomBar.setVisibility(View.INVISIBLE);
                    break;
            }
            return true;
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocalizationPersmissions();
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

    private void getUsersLocation(HttpCallback callback) {
        AtomicReference<JSONArray> result = new AtomicReference<>(new JSONArray());
        JsonArrayRequest request = new JsonArrayRequest
                (Request.Method.GET, API_URL + "getLocations", null, response -> {
                    result.set(response);
                    callback.onSuccess(response);
                },
                        error -> {
                            Toast.makeText(getApplicationContext(), "Cannot update users' location :" + error, Toast.LENGTH_LONG).show();
                        });
        requestQueue.add(request);
    }

    private void refreshUsersLocationOnMap() throws JSONException {
        getUsersLocation(this);
        mMap.clear();
        if (!Objects.equals(usersLocation, null)) {
            for (int i = 0; i < usersLocation.length(); i++) {
                if (usersLocation.getJSONObject(i).getString("username")
                        .equalsIgnoreCase(sharedPreferences.getString("username", "user")))
                    continue;

                UserLocation user = new UserLocation();
                user.setId(Integer.parseInt(usersLocation.getJSONObject(i).get("id").toString()));
                user.setUsername(usersLocation.getJSONObject(i).get("username").toString());
                user.setLast_location_update(new Timestamp(Long.parseLong(usersLocation.getJSONObject(i).get("last_location_update").toString())));
                user.setLatitude(Double.parseDouble(usersLocation.getJSONObject(i).get("latitude").toString()));
                user.setLongitude(Double.parseDouble(usersLocation.getJSONObject(i).get("longitude").toString()));
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                String dateString = formatter.format(new Date(user.getLast_location_update().getTime()));
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(user.getLatitude(), user.getLongitude()))
                        .title(user.getUsername())
                        .snippet(dateString))
                        .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.helmet_small));
            }
        }
    }

    private void updateUserLocation(double latitude, double longitude) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.contains("username")) {
            StringRequest request = new StringRequest
                    (Request.Method.GET, API_URL + "updateUserLocation" +
                            "?username=" + preferences.getString("username", "user" + new Random().nextInt(10000)) +
                            "&latitude=" + latitude +
                            "&longitude=" + longitude,
                            response ->
                                    Log.println(Log.INFO, "RESPONSE", response),
                            error -> {
                                Toast.makeText(getApplicationContext(), "Cannot update current location : " + error, Toast.LENGTH_LONG).show();
                            });
            requestQueue.add(request);
        }
    }

    @Override
    public void onSuccess(JSONArray array) {
        usersLocation = array;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        bottomBar.setVisibility(View.VISIBLE);
        return true;
    }
}

interface HttpCallback {
    public void onSuccess(JSONArray array);
}