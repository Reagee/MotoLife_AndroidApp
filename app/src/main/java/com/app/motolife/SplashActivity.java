package com.app.motolife;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.firebase.TokenUtils;
import com.example.motolife.R;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.app.motolife.URI.API.API_CHECK;

public class SplashActivity extends AppCompatActivity implements CheckCallback {

    AnimatedCircleLoadingView animatedCircleLoadingView;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseAuth auth;
    private RequestQueue requestQueue;
    private boolean state = true;
    private FirebaseUser firebaseUser;
    private TextView checkProgress;
    private TokenUtils tokenUtils;

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
        animatedCircleLoadingView = findViewById(R.id.circle_loading_view);

        startLoading();
        startPercentMockThread();

        try {
            checkInternetPermissions();
            checkConnection(this);
            getUserAuth(this);
            getUserToken(this);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(() -> changeActivity(this), 3000);
    }

    private void changeActivity(CheckCallback checkCallback) {
        checkProgress.setText(getString(R.string.finalizing_text_progress));
        checkCallback.onSuccessCheck(state);
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
    }

    private void startPercentMockThread() {
        Runnable runnable = () -> {
            try {
                Thread.sleep(1500);
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(15);
                    changePercent(i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(runnable).start();
    }

    private void changePercent(final int percent) {
        runOnUiThread(() -> animatedCircleLoadingView.setPercent(percent));
    }

    private void checkInternetPermissions() {
        checkProgress.setText(getString(R.string.check_internet_permissions_text));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != 1) {
            state = false;
        }
    }

    private void checkConnection(CheckCallback checkCallback) {
        checkProgress.setText(getString(R.string.checking_api_connection));
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        response -> {
                            Toast.makeText(getApplicationContext(), "Connection OK", Toast.LENGTH_SHORT).show();
                            checkCallback.onSuccessAPICheck(response);
                        },
                        error -> {
                            Toast.makeText(getApplicationContext(), "Error while connecting to server" +
                                    ", error:" + error, Toast.LENGTH_LONG).show();
                        });
        requestQueue.add(request);
    }

    private void getUserToken(CheckCallback checkCallback) throws ExecutionException, InterruptedException {
        checkProgress.setText(getString(R.string.getting_user_auth_token));
        checkCallback.onSuccessTokenGet(tokenUtils.getFirebaseToken());
    }

    private void getUserAuth(CheckCallback checkCallback) {
        checkProgress.setText(getString(R.string.checking_user_auth));
        authStateListener = firebaseAuth -> {
            firebaseUser = firebaseAuth.getCurrentUser();
            checkCallback.onSuccessAuth(firebaseUser);
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onSuccessAPICheck(String response) {
        if (Objects.isNull(response) || !response.equals("ok"))
            this.state = false;
    }

    @Override
    public void onSuccessTokenGet(String token) {
        if (Objects.isNull(token) || token.isEmpty())
            this.state = false;
    }


    @Override
    public void onSuccessAuth(FirebaseUser firebaseUser) {
        if (Objects.isNull(firebaseUser)) {
            this.state = false;
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }

    @Override
    public void onSuccessCheck(boolean state) {
        if (state) {
            startActivity(new Intent(SplashActivity.this, MapActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
    }
}

interface CheckCallback {
    void onSuccessAPICheck(String response);

    void onSuccessTokenGet(String token);

    void onSuccessAuth(FirebaseUser firebaseUser);

    void onSuccessCheck(boolean state);
}