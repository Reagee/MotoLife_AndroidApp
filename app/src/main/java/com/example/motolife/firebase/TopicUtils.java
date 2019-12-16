package com.example.motolife.firebase;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.atomic.AtomicReference;

public class TopicUtils {

    public static String subscribeToTopic(String topic){
        AtomicReference<String> msg = new AtomicReference<>("");
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(
                task -> {
                    msg.set("Subscribed to " + topic + " topic.");
                    if (!task.isSuccessful())
                        msg.set("Failed to subscribed to " + topic + " topic.");
                }
        );
        return msg.get();
    }
}
