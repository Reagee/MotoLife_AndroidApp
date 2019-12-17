package com.app.motolife.firebase;

import android.util.Log;

import com.example.motolife.R;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public class TokenUtils {
    private static final String TAG = "TokenUtils";

    public static String getFirebaseToken(){
        AtomicReference<String> msg = new AtomicReference<>("");
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    String token = Objects.requireNonNull(task.getResult()).getToken();

                    msg.set(R.string.msg_token_fmt+" : "+token);
                    Log.d(TAG, msg.get());
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        return msg.get();
    }

}
