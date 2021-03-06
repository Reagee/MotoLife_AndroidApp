package com.app.motolife.maputils;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.Notifications.SoundService;
import com.app.motolife.URI.PowerOffController;
import com.app.motolife.chat.ChatActivity;
import com.app.motolife.chat.MessageActivity;
import com.app.motolife.firebase.FirebaseUtils;
import com.app.motolife.firebase.MyFirebaseMessagingService;
import com.app.motolife.firebase.UserStatus;
import com.app.motolife.model.Chat;
import com.app.motolife.model.User;
import com.app.motolife.model.UserLocation;
import com.app.motolife.user.GpsStatusHandler;
import com.app.motolife.user.LocalizationPermissionActivity;
import com.app.motolife.user.LoginActivity;
import com.app.motolife.user.ProfileActivity;
import com.bumptech.glide.Glide;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.hdodenhof.circleimageview.CircleImageView;

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
        HttpCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private RequestQueue requestQueue;
    private JSONArray usersLocation;
    private MeowBottomNavigation meowBottomNavigation;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private String globalUsername;
    private Marker clickedMarker;
    private FirebaseUtils firebaseUtils;

    private final boolean[] exitAppFlag = new boolean[]{false};

    private boolean darkModeEnabled = false;
    private TextView infoMessageBox;
    private String userId;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_map);
        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        firebaseUtils = FirebaseUtils.getInstance();

        String channelId = getString(R.string.default_notification_channel_id);
        String channelName = getString(R.string.default_notification_channel_name);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(new NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW));
        Intent intent = new Intent(MapActivity.this, MyFirebaseMessagingService.class);
        startForegroundService(intent);


        requestQueue = Volley.newRequestQueue(getApplicationContext());
        PowerManager manager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock =
                Objects.requireNonNull(manager).newWakeLock(PARTIAL_WAKE_LOCK, getString(R.string.RIDING_WAKE_LOCK));
        FirebaseAuth firebaseAuth = firebaseUtils.getFirebaseAuth();
        firebaseAuth.addAuthStateListener(authStateListener);
        getUsernameAtStart(this);

        checkLocalizationPermissions();
        initializeMap();
    }


    private void checkLocalizationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            startActivity(new Intent(MapActivity.this, LocalizationPermissionActivity.class).setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP));
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

        FloatingActionMenu actionMenu = findViewById(R.id.action_menu);
        actionMenu.getMenuIconView().setImageResource(R.drawable.menu_float_white);
        actionMenu.setIconAnimated(false);
        actionMenu.setClosedOnTouchOutside(true);

        FloatingActionButton addEvent = findViewById(R.id.add_event_button);
        FloatingActionButton checkEvents = findViewById(R.id.events_button);
        FloatingActionButton viewMessages = findViewById(R.id.messages_button);
        FloatingActionButton darkMode = findViewById(R.id.dark_mode_button);
        infoMessageBox = findViewById(R.id.error_message_info_box);

        CircleImageView profileImage = findViewById(R.id.profile_image);
        CircleImageView notificationIndicator = findViewById(R.id.notification_ind);

        DatabaseReference databaseReference;
        databaseReference = FirebaseDatabase.getInstance().getReference("chat");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unread = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (Objects.requireNonNull(chat).getReceiver()
                            .equals(firebaseUtils.getFirebaseUser().getUid()) && !chat.isIsseen()) {
                        unread++;
                    }
                }
                if (unread > 0)
                    notificationIndicator.setVisibility(View.VISIBLE);
                else
                    notificationIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users").child(firebaseUtils.getFirebaseUser().getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (!Objects.equals(user.getImageURL(), "default"))
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        profileImage.setOnClickListener(click -> {
            startActivity(new Intent(this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP));
        });

        darkMode.setOnClickListener(click -> {
            if (darkModeEnabled) {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.standard_map));
                darkMode.setLabelText("Enable dark mode");
                darkModeEnabled = false;
            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map));
                darkMode.setLabelText("Disable dark mode");
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#202C38"));
                gd.setStroke(1, 0xFF000000);
                darkModeEnabled = true;
            }
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

    @RequiresApi(api = Build.VERSION_CODES.P)
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
                    Intent intent = new Intent(getApplicationContext(), MessageActivity.class);
                    intent.putExtra("userid", userId);
                    startActivity(intent);
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "User Poked !", Toast.LENGTH_SHORT).show();
                    new SoundService(this).makePokeSound();
//                    new UserControlUtils(new UserPoke(firebaseUser.getUid(), userId), this).pokeUser();
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

    private void checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gps_enabled || !isNetworkEnabled) {
            startActivity(new Intent(MapActivity.this, GpsStatusHandler.class).setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP));
        }
    }

    @Override
    public void onDestroy() {
        UserStatus.OFFLINE();
        if (mGpsSwitchStateReceiver != null)
            unregisterReceiver(mGpsSwitchStateReceiver);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        UserStatus.OFFLINE();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserStatus.ONLINE();
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
        @SuppressLint("SetTextI18n") JsonArrayRequest request = new JsonArrayRequest
                (Request.Method.GET, API_GET_LOCATIONS, null, response -> {
                    result.set(response);
                    callback.onSuccess(response);
                    infoMessageBox.setBackgroundResource(R.color.Green);
                    infoMessageBox.setText(R.string.connection_established);
                    new Handler().postDelayed(() -> infoMessageBox.setVisibility(View.GONE), 1000);
                },
                        error -> {
                            infoMessageBox.setBackgroundResource(R.color.Red);
                            infoMessageBox.setText(R.string.connection_error);
                            infoMessageBox.setVisibility(View.VISIBLE);
                        });
        requestQueue.add(request);
    }

    private void refreshUsersLocationOnMap() throws JSONException {
        getUsersLocation(this);
        mMap.clear();
        if (!Objects.equals(usersLocation, null)) {
            for (int i = 0; i < usersLocation.length(); i++) {
                if (usersLocation.getJSONObject(i).getString("email")
                        .equalsIgnoreCase(firebaseUtils.getFirebaseUser().getEmail()))
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
        return (darkModeEnabled) ? BitmapDescriptorFactory.fromResource(R.drawable.helmet_small_white) :
                BitmapDescriptorFactory.fromResource(R.drawable.helmet_small);
    }

    private void updateUserLocation(double latitude, double longitude) {

        if (!Objects.equals(firebaseUtils.getFirebaseUser(), null)) {
            StringRequest request = new StringRequest
                    (Request.Method.GET, API_GET_UPDATE_USER_LOCATION +
                            "?email=" + firebaseUtils.getFirebaseUser().getEmail() +
                            "&latitude=" + latitude +
                            "&longitude=" + longitude,
                            response -> {
                                Log.println(Log.INFO, "RESPONSE", response);
                                infoMessageBox.setBackgroundResource(R.color.Green);
                                infoMessageBox.setText(R.string.connection_established);
                                new Handler().postDelayed(() -> infoMessageBox.setVisibility(View.GONE), 1000);
                            },
                            error -> {
                                infoMessageBox.setBackgroundResource(R.color.Red);
                                infoMessageBox.setText(R.string.connection_error);
                                infoMessageBox.setVisibility(View.VISIBLE);
                            });
            requestQueue.add(request);
        }
    }

    private void getUsernameAtStart(HttpCallback callback) {
        StringRequest request = new StringRequest
                (Request.Method.GET, API_GET_UPDATE_USERNAME + "?email=" + firebaseUtils.getFirebaseUser().getEmail(),
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
            globalUsername = username;
        } else {
            getUsernameAtStart(this);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (Objects.equals(user.getUsername(), marker.getTitle()))
                        userId = user.getId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

    FirebaseAuth.AuthStateListener authStateListener = firebaseAuth -> {
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MapActivity.this, LoginActivity.class));
            finish();
        }
    };
}

interface HttpCallback {
    void onSuccess(JSONArray array);

    void onSuccessUsernameGet(String username);
}