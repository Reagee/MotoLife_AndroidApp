package com.app.motolife.firebase;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.app.motolife.Notifications.Token;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.ExecutionException;


public class TokenUtils implements TokenCallback {

    private static final String TAG = "TokenUtils";
    private String token;

    public TokenUtils() {
        getFirebaseToken(this);
    }

    private void getFirebaseToken(TokenCallback callback) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "getInstanceId failed", task.getException());
                return;
            }
            String token = Objects.requireNonNull(task.getResult()).getToken();
            callback.onSuccessTokenGet(token);
        });
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
    }

    public Token getFirebaseToken() throws ExecutionException, InterruptedException {
        return (Objects.isNull(token)) ? new Token("User token did not get") : new Token(token);
    }

    @Override
    public void onSuccessTokenGet(String token) {
        this.token = token;
    }

}

interface TokenCallback {
    void onSuccessTokenGet(String token);
}