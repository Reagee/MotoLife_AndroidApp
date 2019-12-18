package com.app.motolife.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

import androidx.annotation.NonNull;


public class TokenUtils implements TokenCallback {

    private static final String TAG = "TokenUtils";
    private String token;

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


    public String getFirebaseToken() {
        if (Objects.isNull(this.token) || this.token.isEmpty())
            getFirebaseToken(this);
        return this.token;
    }

    @Override
    public void onSuccessTokenGet(String token) {
        this.token = token;
    }

    public boolean isExecuting() {
        return Objects.isNull(this.token);
    }

    public String getToken() {
        return this.token;
    }
}

interface TokenCallback {
    void onSuccessTokenGet(String token);
}