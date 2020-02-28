package com.app.motolife;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.firebase.TokenUtils;
import com.example.motolife.R;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.app.motolife.URI.API.API_CHECK;

public class SplashActivity extends AppCompatActivity {

    private AnimatedCircleLoadingView animatedCircleLoadingView;
    private RequestQueue requestQueue;
    private boolean connectionFlag = true;
    private boolean authFlag = true;
    private String APIResponse;
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

        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);
        checkProgress.setText(getString(R.string.Loading));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);

        checkConnection();
        checkIfLocalizationIsEnabled();
        getUserAuth();
        getUserToken();

        changeActivity();
    }

    private void changeActivity() {
        checkProgress.setText(getString(R.string.finalizing_text_progress));
//        for (int i = 0; i < 10; i++) {
//            if (!Objects.equals(APIResponse,"ok")) {
//                try {
//                    Thread.sleep(1000);
//                    connectionFlag = false;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                connectionFlag = true;
//                break;
//            }
//        }

        if (connectionFlag && authFlag) {
            startActivity(new Intent(getApplicationContext(), MapActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else if (!connectionFlag) {
            finish();
            startActivity(getIntent());
        } else {
            finish();
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
    }

    private void checkIfLocalizationIsEnabled() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled;
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gps_enabled) {
            connectionFlag = false;
            startActivityForResult(new Intent(SplashActivity.this, GpsStatusHandler.class).putExtra("activity", "splash"), 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK)
            connectionFlag = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != 1) {
            connectionFlag = false;
        }
    }

    private void checkConnection() {
        RequestFuture<String> result = RequestFuture.newFuture();
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        this::checkAPI,
                        error -> {
                            connectionFlag = false;
                            APIResponse = "error";
                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
        requestQueue.add(request);
    }

    private void getUserToken() {
        if (Objects.isNull(tokenUtils.getFirebaseToken()))
            connectionFlag = false;
    }

    private void getUserAuth() {
        if (Objects.isNull(FirebaseAuth.getInstance().getCurrentUser()))
            authFlag = false;
    }

    public void checkAPI(String res) {
        if (!Objects.equals(res, "ok")) {
            connectionFlag = false;
            APIResponse = "error";
            Toast.makeText(getApplicationContext(), "Server connection error!", Toast.LENGTH_SHORT).show();
        }
    }
}
