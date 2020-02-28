package com.app.motolife;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.firebase.TokenUtils;
import com.example.motolife.R;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import static com.app.motolife.URI.API.API_CHECK;

public class SplashActivity extends AppCompatActivity implements APICallback {

    AnimatedCircleLoadingView animatedCircleLoadingView;
    private RequestQueue requestQueue;
    private boolean connectionFlag = true;
    private boolean authFlag = true;
    private TextView checkProgress;
    private TokenUtils tokenUtils;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);
        checkProgress = findViewById(R.id.checkProgressText);
        progressBar = findViewById(R.id.spin_kit);

        tokenUtils = new TokenUtils();
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        //starting the animation
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);

        synchronized (this) {
            checkProgress.setText(getString(R.string.Loading));
            connectionFlag = checkInternetPermissions() &&
                    checkConnection(this) &&
                    getUserAuth() &&
                    getUserToken() &&
                    checkIfLocalizationIsEnabled();
        }
        new Handler().postDelayed(this::changeActivity, 2000);
    }

    private void changeActivity() {
        checkProgress.setText(getString(R.string.finalizing_text_progress));
        new Handler().postDelayed(() -> {
            if (connectionFlag && authFlag) {
                startActivity(new Intent(getApplicationContext(), MapActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } else if (!connectionFlag) {
                finish();
                startActivity(getIntent());
            } else {
                finish();
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        }, 2000);
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
    }

    private boolean checkInternetPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            this.connectionFlag = false;
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
        }
        return connectionFlag;
    }

    private boolean checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gps_enabled) {
            connectionFlag = false;
            startActivity(new Intent(SplashActivity.this, GpsStatusHandler.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }

        return connectionFlag;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 1) {
            connectionFlag = false;
        }
    }

    private boolean checkConnection(APICallback callback) {
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        callback::checkAPI,
                        error -> this.connectionFlag = false);
        requestQueue.add(request);
        return this.connectionFlag;
    }

    private boolean getUserToken() {
        return Objects.nonNull(tokenUtils.getFirebaseToken());
    }

    private boolean getUserAuth() {
        return (Objects.nonNull(FirebaseAuth.getInstance().getCurrentUser()));
    }

    @Override
    public void checkAPI(String res) {
        if (!Objects.equals(res, "ok"))
            this.connectionFlag = false;
    }
}

interface APICallback {
    void checkAPI(String res);
}