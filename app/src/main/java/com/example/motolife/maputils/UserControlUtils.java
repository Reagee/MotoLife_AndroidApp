package com.example.motolife.maputils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class UserControlUtils {

    public static void pokeUser(String username){
        RemoteMessage message = new RemoteMessage.Builder("Poke")
                .addData("New Poke !", "User " + username + " has just Poked you !")
                .setTtl(3600)
                .setMessageType("Poke")
                .build();
        FirebaseMessaging.getInstance().send(message);
    }
}
