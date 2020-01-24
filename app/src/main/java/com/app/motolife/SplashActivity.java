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
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.firebase.TokenUtils;
import com.example.motolife.R;
import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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

        //starting the animation
        startLoading();
        startPercentMockThread();

//        Thread[] checkers = new Thread[4];
//        checkers[0] = new Thread(this::checkInternetPermissions);
//        checkers[1] = new Thread(this::checkConnection);
//        checkers[2] = new Thread(this::getUserAuth);
//        checkers[3] = new Thread(this::getUserToken);
//
//        for (Thread t : checkers) {
//            try {
//                t.start();
//                t.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        checkInternetPermissions();
        checkConnection(this);
        getUserAuth();
        getUserToken();

        new Handler().postDelayed(this::changeActivity, 3000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
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

    private void changeActivity() {
        checkProgress.setText(getString(R.string.finalizing_text_progress));
        if (connectionFlag && authFlag) {
            startActivity(new Intent(SplashActivity.this, MapActivity.class));
        } else if (!connectionFlag) {
            Toast.makeText(getApplicationContext(), "Error occurred.", Toast.LENGTH_LONG).show();
            finish();
            startActivity(getIntent());
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
    }


    private void startLoading() {
        animatedCircleLoadingView.startDeterminate();
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
            connectionFlag = false;
        }
    }

    private void checkConnection(APICallback callback) {
        checkProgress.setText(getString(R.string.checking_api_connection));
        StringRequest request = new StringRequest
                (Request.Method.GET, API_CHECK,
                        callback::checkAPI,
                        error -> {
                            this.connectionFlag = false;
                        });
        requestQueue.add(request);
    }

    private void getUserToken() {
        checkProgress.setText(getString(R.string.getting_user_auth_token));
        String token = null;
        try {
            token = tokenUtils.getFirebaseToken();
        } catch (ExecutionException | InterruptedException e) {
            this.connectionFlag = false;
        }
        if (Objects.isNull(token) || Objects.requireNonNull(token).isEmpty())
            this.connectionFlag = false;
    }

    private void getUserAuth() {
        checkProgress.setText(getString(R.string.checking_user_auth));
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