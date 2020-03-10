package com.app.motolife.user;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Button;

import com.app.motolife.SplashActivity;
import com.example.motolife.R;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

public class GpsStatusHandler extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_status_handler);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        Button gpsHandlerButton = findViewById(R.id.gps_handler_button);

        gpsHandlerButton.setOnClickListener(click -> {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(intent);
        });

    }

    private BroadcastReceiver mGpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).matches("android.location.PROVIDERS_CHANGED")) {
                checkIfLocalizationIsEnabled();
            }
        }
    };

    private void checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = Objects.requireNonNull(lm).isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        String activity = Objects.requireNonNull(getIntent().getExtras()).getString("activity", "splash");

        if (gps_enabled || isNetworkEnabled) {
            if (activity.equals("splash")) {
                setResult(RESULT_OK);
                finish();
                startActivity(new Intent(GpsStatusHandler.this, SplashActivity.class));
            } else
                finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.KEY_PROVIDER_ENABLED);
        registerReceiver(mGpsSwitchStateReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGpsSwitchStateReceiver);
    }


}
