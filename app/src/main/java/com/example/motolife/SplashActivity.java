package com.example.motolife;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.motolife.firebase.TokenUtils;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.motolife.URI.API.API_CHECK;

public class SplashActivity extends AppCompatActivity {

    AnimatedCircleLoadingView animatedCircleLoadingView;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseAuth auth;
    private RequestQueue requestQueue;
    //    private static final String API_URL = "http://s1.ct8.pl:25500/";
    private boolean state = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_splash);
        auth = FirebaseAuth.getInstance();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        animatedCircleLoadingView = findViewById(R.id.circle_loading_view);
        startLoading();
        try {
            Thread.sleep(1500);
            checkConnection();
            getUserAuth();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        changeActivity(state);
    }

    private void changeActivity(boolean state) {
        if (state)
            startActivity(new Intent(SplashActivity.this, MapActivity.class));
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
                    if (i == 100)
                        startActivity(new Intent(SplashActivity.this, MapActivity.class));
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

    private void checkConnection() throws InterruptedException {
        Thread.sleep(50);
        changePercent(33);
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        response -> Toast.makeText(getApplicationContext(), "Connection OK", Toast.LENGTH_SHORT).show(),
                        error -> Toast.makeText(getApplicationContext(), "Error while connecting to server" +
                                ", error:" + error, Toast.LENGTH_LONG).show());
        requestQueue.add(request);
    }

    private void getUserToken() {
        Toast.makeText(this, TokenUtils.getFirebaseToken(), Toast.LENGTH_SHORT).show();
    }

    private void getUserAuth() throws InterruptedException {
        Thread.sleep(50);
        changePercent(100);
        authStateListener = firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (Objects.nonNull(firebaseUser))
                startActivity(new Intent(SplashActivity.this, MapActivity.class));
            else
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }
}
