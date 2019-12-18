package com.app.motolife.firebase;

import android.util.Log;

import com.example.motolife.R;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public class TokenUtils implements TokenCallback {

    private static TokenUtils tokenUtils;
    private static final String TAG = "TokenUtils";
    private String token;

    private TokenUtils() {
    }

    private void getFirebaseToken(TokenCallback callback) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    String token = Objects.requireNonNull(task.getResult()).getToken();
                    callback.onSuccessTokenGet(token);
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
    }

    public static TokenUtils getInstance() {
        if (Objects.isNull(tokenUtils))
            tokenUtils = new TokenUtils();
        return tokenUtils;
    }

    public String getFirebaseToken() {
        if (Objects.isNull(token) || token.isEmpty())
            getFirebaseToken(this);
        return this.token;
    }

    @Override
    public void onSuccessTokenGet(String token) {
        this.token = token;
    }
}

interface TokenCallback {
    void onSuccessTokenGet(String token);
}