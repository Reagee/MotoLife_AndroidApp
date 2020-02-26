package com.app.motolife;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.Notifications.Token;
import com.app.motolife.firebase.TokenUtils;
import com.example.motolife.R;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.app.motolife.URI.API.API_CHECK;

public class SplashActivity extends AppCompatActivity implements APICallback {

    AnimatedCircleLoadingView animatedCircleLoadingView;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseAuth auth;
    private RequestQueue requestQueue;
    private boolean connectionFlag = true;
    private boolean authFlag = true;
    private FirebaseUser firebaseUser;
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
        auth = FirebaseAuth.getInstance();
        tokenUtils = new TokenUtils();

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        progressBar = findViewById(R.id.spin_kit);

        //starting the animation
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);

        synchronized (this) {
            checkProgress.setText(getString(R.string.Loading));
            checkInternetPermissions();
            checkConnection(this);
            getUserAuth();
            getUserToken();
            checkIfLocalizationIsEnabled();
        }
        new Handler().postDelayed(this::changeActivity, 2000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }


    private void changeActivity() {
        checkProgress.setText(getString(R.string.finalizing_text_progress));
        new Handler().postDelayed(() -> {
            if (connectionFlag && authFlag) {
                startActivity(new Intent(SplashActivity.this, MapActivity.class));
            } else if (!connectionFlag) {
                Toast.makeText(getApplicationContext(), "Error occurred.", Toast.LENGTH_LONG).show();
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
        }, 2000);
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
    }

    private void checkInternetPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
    }

    private void checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;

        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gps_enabled) {
            startActivity(new Intent(SplashActivity.this, GpsStatusHandler.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        }
        else{
            connectionFlag = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 1) {
            connectionFlag = false;
        }
    }

    private void checkConnection(APICallback callback) {
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        callback::checkAPI,
                        error -> this.connectionFlag = false);
        requestQueue.add(request);
    }

    private void getUserToken() {
        Token token = null;
        try {
            token = tokenUtils.getFirebaseToken();
        } catch (ExecutionException | InterruptedException e) {
            this.connectionFlag = false;
        }
        if (Objects.isNull(token) || Objects.requireNonNull(token.getToken()).isEmpty())
            this.connectionFlag = false;
    }

    private void getUserAuth() {
        authStateListener = firebaseAuth -> {
            firebaseUser = firebaseAuth.getCurrentUser();
            if (Objects.isNull(firebaseUser) || Objects.requireNonNull(firebaseUser.getEmail()).isEmpty()) {
                this.authFlag = false;
            }
        };
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