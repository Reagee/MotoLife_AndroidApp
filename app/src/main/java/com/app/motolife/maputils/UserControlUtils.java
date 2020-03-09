package com.app.motolife.maputils;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import com.app.motolife.Notifications.Client;
import com.app.motolife.Notifications.OreoNotification;
import com.app.motolife.chat.MessageActivity;
import com.app.motolife.firebase.APIService;
import com.app.motolife.model.User;
import com.app.motolife.model.UserPoke;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;

public class UserControlUtils {

    private APIService apiService;
    private FirebaseUser firebaseUser;
    private DatabaseReference reference;
    private UserPoke userPoke;
    private String userid;
    private Activity activity;

    public UserControlUtils(UserPoke userPoke, Activity activity) {
        this.firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        this.reference = FirebaseDatabase.getInstance().getReference("pokes");
        this.userPoke = userPoke;
        this.userid = userPoke.getReceiverId();
        this.activity = activity;
    }

    public void pokeUser() {

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> pokeMap = new HashMap<>();
        pokeMap.put("sender", userPoke.getSenderId());
        pokeMap.put("receiver", userPoke.getReceiverId());

        reference.child("pokes").push().setValue(pokeMap);

        reference = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                sendOreoNotification(userPoke, user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendOreoNotification(UserPoke userPoke, User usr) {
        String user = usr.getUsername();
        String icon = usr.getImageURL();
        String title = "New poke!";
        String body = "You have been poked!";

        int j = Integer.parseInt(Objects.requireNonNull(user).replaceAll("[\\D]", ""));
        Intent intent = new Intent(activity, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(activity, j, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        OreoNotification oreoNotification = new OreoNotification(activity);
        Notification.Builder builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon);

        int i = 0;
        if (j > 0) {
            i = j;
        }
        oreoNotification.getNotificationManager().notify(i, builder.build());
    }
}
