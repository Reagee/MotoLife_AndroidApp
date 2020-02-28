package com.app.motolife.firebase;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class TopicUtils implements SubCallback{

    private String message;
    private static TopicUtils topicUtils;

    private TopicUtils(){}

    public static void subscribeToTopic(String topic, SubCallback callback){
        AtomicReference<String> msg = new AtomicReference<>();
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener(
                task -> {
                    msg.set("Subscribed to " + topic + " topic.");
                    Log.println(Log.INFO,"SUBSCRIPTION TO TOPIC: ",topic);
                    if (!task.isSuccessful())
                        msg.set("Failed to subscribed to " + topic + " topic.");
                        callback.onSuccessSubscribe(msg.get());
                }
        );
    }

    @Override
    public void onSuccessSubscribe(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }

    public static TopicUtils getInstance(){
        if(Objects.isNull(topicUtils))
            topicUtils = new TopicUtils();
        return topicUtils;
    }
}

interface SubCallback{
    void onSuccessSubscribe(String message);
}
