package com.app.motolife;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);
        checkProgress = findViewById(R.id.checkProgressText);
        auth = FirebaseAuth.getInstance();

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        animatedCircleLoadingView = findViewById(R.id.circle_loading_view);

        startLoading();
        startPercentMockThread();

        checkInternetPermissions();
        checkConnection(this);
        getUserAuth(this);
        getUserToken(this);

        new Handler().postDelayed(()->changeActivity(this),2000);
    }

    private void changeActivity(CheckCallback checkCallback) {
        checkProgress.setText("Finalizing...");
        new Handler().postDelayed(() -> checkCallback.onSuccesCheck(state), 200);
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
    }

    private void startPercentMockThread() {
        Runnable runnable = () -> {
            try {
                Thread.sleep(1500);
                for (int i = 0; i <= 100; i++) {
                    Thread.sleep(5);
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
            new Handler().postDelayed(() -> requestPermissions(new String[]{Manifest.permission.INTERNET}, 1), 200);
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
        new Handler().postDelayed(() -> requestQueue.add(request), 1000);
    }

    private void getUserToken(CheckCallback checkCallback) {
        checkProgress.setText(getString(R.string.getting_user_auth_token));
        String token = TokenUtils.getFirebaseToken();
        Toast.makeText(this, token, Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(() -> checkCallback.onSuccessTokenGet(token), 1000);
    }

    private void getUserAuth(CheckCallback checkCallback) {
        checkProgress.setText(getString(R.string.checking_user_auth));
        authStateListener = firebaseAuth -> {
            firebaseUser = firebaseAuth.getCurrentUser();
            if (Objects.isNull(firebaseUser) || Objects.requireNonNull(firebaseUser).getEmail().isEmpty())
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        };
        new Handler().postDelayed(() -> checkCallback.onSuccessAuth(firebaseUser), 1000);
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
        if (Objects.isNull(firebaseUser))
            this.state = false;
    }

    @Override
    public void onSuccesCheck(boolean state) {
        if (state)
            startActivity(new Intent(SplashActivity.this, MapActivity.class));
        else
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
    }
}

interface CheckCallback {
    void onSuccessAPICheck(String response);

    void onSuccessTokenGet(String token);

    void onSuccessAuth(FirebaseUser firebaseUser);

    void onSuccesCheck(boolean state);
}