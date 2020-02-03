package com.app.motolife;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.URI.PowerOffController;
import com.app.motolife.chat.ChatActivity;
import com.app.motolife.firebase.MyFirebaseMessagingService;
import com.app.motolife.firebase.TopicUtils;
import com.app.motolife.maputils.UserControlUtils;
import com.app.motolife.ui.SoundService;
import com.app.motolife.ui.model.UserLocation;
import com.etebarian.meowbottomnavigation.MeowBottomNavigation;
import com.example.motolife.R;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static com.app.motolife.URI.API.API_GET_LOCATIONS;
import static com.app.motolife.URI.API.API_GET_UPDATE_USERNAME;
import static com.app.motolife.URI.API.API_GET_UPDATE_USER_LOCATION;
import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMyLocationClickListener,
        HttpCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private RequestQueue requestQueue;
    private JSONArray usersLocation;
    private Switch darkModeSwitch;
    private MeowBottomNavigation meowBottomNavigation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private String globalUsername;
    private Marker clickedMarker;
    //    private TextView bottomNavBarText;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private TopicUtils topicUtils;

    private Button logoutButton;
    private FloatingActionMenu actionMenu;
    private FloatingActionButton addEvent;
    private FloatingActionButton checkEvents;
    private FloatingActionButton viewMessages;
    private final boolean[] exitAppFlag = new boolean[]{false};

    private FrameLayout actionLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_map);
        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        String channelId = getString(R.string.default_notification_channel_id);
        String channelName = getString(R.string.default_notification_channel_name);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW));
        Intent intent = new Intent(MapActivity.this, MyFirebaseMessagingService.class);
        startService(intent);


        requestQueue = Volley.newRequestQueue(getApplicationContext());
        PowerManager manager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock =
                Objects.requireNonNull(manager).newWakeLock(PARTIAL_WAKE_LOCK, getString(R.string.RIDING_WAKE_LOCK));
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        getUsernameAtStart(this);

        checkLocalizationPermissions();
        initializeMap();
    }


    private void checkLocalizationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            startActivity(new Intent(MapActivity.this, LocalizationPermissionActivity.class));
    }

    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).matches("android.location.PROVIDERS_CHANGED")) {
                checkIfLocalizationIsEnabled();
            }
        }
    };

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
        try {
            refreshUsersLocationOnMap();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationsList = locationResult.getLocations();
            if (locationsList.size() > 0) {
                Location location = locationsList.get(locationsList.size() - 1);
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

        actionMenu = findViewById(R.id.action_menu);
        addEvent = findViewById(R.id.add_event_button);
        checkEvents = findViewById(R.id.events_button);
        viewMessages = findViewById(R.id.messages_button);

        logoutButton = findViewById(R.id.logout);
        actionLayout = findViewById(R.id.action_layout);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.standard_map));
                darkModeSwitch.setTextColor(Color.BLACK);
                logoutButton.setBackground(getDrawable(R.drawable.logout_dark));

            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map));
                darkModeSwitch.setTextColor(Color.WHITE);
                logoutButton.setBackground(getDrawable(R.drawable.logout_white));

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#202C38"));
                gd.setStroke(1, 0xFF000000);
            }
        });

        logoutButton.setOnClickListener(click -> {
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage("Are you sure ?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        firebaseAuth.signOut();
                        startActivity(new Intent(MapActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .show();
        });

        addEvent.setOnClickListener(click -> {

        });

        checkEvents.setOnClickListener(click -> {

        });

        viewMessages.setOnClickListener(click -> {
             startActivity(new Intent(MapActivity.this, ChatActivity.class).setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP));
        });

    }


    @Override
    public void onBackPressed() {
        PowerOffController.powerOff(exitAppFlag, this);
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
        Task<Location> startLocation = LocationServices.getFusedLocationProviderClient(getApplicationContext()).getLastLocation();
        startLocation.addOnSuccessListener(listener -> {
            if (listener != null) {
                LatLng loc = new LatLng(
                        Objects.requireNonNull(startLocation.getResult()).getLatitude(),
                        Objects.requireNonNull(startLocation.getResult()).getLongitude()
                );
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
        Objects.requireNonNull(mapFragment).getMapAsync(this);
        meowBottomNavigation = findViewById(R.id.meow_bottom_nav);
        meowBottomNavigation.add(new MeowBottomNavigation.Model(1, R.drawable.ic_insert_comment_black_24dp));
        Objects.requireNonNull(meowBottomNavigation.getModelById(1)).setCount("Message");
        meowBottomNavigation.add(new MeowBottomNavigation.Model(2, R.drawable.ic_child_care_black_24dp));
        Objects.requireNonNull(meowBottomNavigation.getModelById(2)).setCount("Poke");
        meowBottomNavigation.add(new MeowBottomNavigation.Model(3, R.drawable.ic_close_black_24dp));
        Objects.requireNonNull(meowBottomNavigation.getModelById(3)).setCount("Exit");

        meowBottomNavigation.setOnClickMenuListener(model -> {
            switch (model.getId()) {
                case 1:
                    Toast.makeText(getApplicationContext(), "User messaged !", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "User Poked !", Toast.LENGTH_SHORT).show();
                    new SoundService(this).makePokeSound();
                    subscribeToTopic(clickedMarker.getTitle());
                    pokeUser();
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(clickedMarker.getTitle());
                    break;
                case 3:
                    meowBottomNavigation.setVisibility(View.INVISIBLE);
                    meowBottomNavigation.setTranslationY(80.0f);
                    clickedMarker.hideInfoWindow();
                    break;
            }
            return null;
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocalizationPermissions();
    }

    private void pokeUser() {
        UserControlUtils.pokeUser(this.globalUsername);
    }

    private void subscribeToTopic(String topic) {
        topicUtils = TopicUtils.getInstance();
        TopicUtils.subscribeToTopic(topic, topicUtils);
        Toast.makeText(getApplicationContext(), topicUtils.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gps_enabled) {
            finish();
            startActivity(new Intent(MapActivity.this, GpsStatusHandler.class));
        }
    }

    @Override
    public void onDestroy() {
        if (mGpsSwitchStateReceiver != null)
            unregisterReceiver(mGpsSwitchStateReceiver);
        super.onDestroy();
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
                (Request.Method.GET, API_GET_LOCATIONS, null, response -> {
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
                if (usersLocation.getJSONObject(i).getString("email")
                        .equalsIgnoreCase(firebaseUser.getEmail()))
                    continue;

                UserLocation user = new UserLocation();
                user.setId(Integer.parseInt(usersLocation.getJSONObject(i).get("id").toString()));
                user.setUsername(usersLocation.getJSONObject(i).get("username").toString());
                user.setEmail(usersLocation.getJSONObject(i).get("email").toString());
                user.setLast_location_update(new Timestamp(Long.parseLong(usersLocation.getJSONObject(i).get("last_location_update").toString())));
                user.setLatitude(Double.parseDouble(usersLocation.getJSONObject(i).get("latitude").toString()));
                user.setLongitude(Double.parseDouble(usersLocation.getJSONObject(i).get("longitude").toString()));
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                String dateString = formatter.format(new Date(user.getLast_location_update().getTime()));
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(user.getLatitude(), user.getLongitude()))
                        .title(user.getUsername()))
                        .setIcon(chooseProperMarkerBg());
            }
        }
    }

    private BitmapDescriptor chooseProperMarkerBg() {
        return (darkModeSwitch.isChecked()) ? BitmapDescriptorFactory.fromResource(R.drawable.helmet_small_white) :
                BitmapDescriptorFactory.fromResource(R.drawable.helmet_small);
    }

    private void updateUserLocation(double latitude, double longitude) {

        if (!Objects.equals(firebaseUser, null)) {
            StringRequest request = new StringRequest
                    (Request.Method.GET, API_GET_UPDATE_USER_LOCATION +
                            "?email=" + firebaseUser.getEmail() +
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

    private void getUsernameAtStart(HttpCallback callback) {
        StringRequest request = new StringRequest
                (Request.Method.GET, API_GET_UPDATE_USERNAME + "?email=" + firebaseUser.getEmail(),
                        response -> {
                            Log.println(Log.INFO, "RESPONSE SUBSCRIBE: ", response);
                            callback.onSuccessUsernameGet(response);
                        },
                        error -> Toast.makeText(getApplicationContext(),
                                "Cannot subscribe to topic: " + error, Toast.LENGTH_LONG).show()
                );
        requestQueue.add(request);
    }

    @Override
    public void onSuccess(JSONArray array) {
        usersLocation = array;
    }


    @Override
    public void onSuccessUsernameGet(String username) {
        if (!Objects.equals(username, null)) {
            subscribeToTopic(username);
            this.globalUsername = username;
        } else {
            getUsernameAtStart(this);
            subscribeToTopic(this.globalUsername);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        meowBottomNavigation.setVisibility(View.VISIBLE);
        meowBottomNavigation.setTranslationY(0);
        clickedMarker = marker;
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                .target(clickedMarker.getPosition())
                .zoom(mMap.getCameraPosition().zoom)
                .tilt(mMap.getCameraPosition().tilt)
                .bearing(mMap.getCameraPosition().bearing)
                .build()));
        if (!clickedMarker.isInfoWindowShown())
            clickedMarker.showInfoWindow();
        return true;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }
}

interface HttpCallback {
    void onSuccess(JSONArray array);

    void onSuccessUsernameGet(String username);
}