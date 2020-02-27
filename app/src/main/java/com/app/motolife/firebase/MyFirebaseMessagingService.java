package com.app.motolife.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.Notifications.OreoNotification;
import com.app.motolife.Notifications.Token;
import com.app.motolife.chat.MessageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import static com.app.motolife.URI.API.API_SET_USER_TOKEN;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String sent = remoteMessage.getData().get("sent");
        String user = remoteMessage.getData().get("user");

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String currentUser = preferences.getString("currentuser", "none");

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (!Objects.equals(firebaseUser, null) && Objects.equals(sent, firebaseUser.getUid())) {
            if (!Objects.equals(currentUser, user)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    sendOreoNotification(remoteMessage);
                else
                    sendNotification(remoteMessage);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.println(Log.INFO, "Refreshed token: ", token);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (!Objects.equals(firebaseUser, null)) {
            updateToken(token);
            sendRegistrationToServer(token);
        }
    }

    private void updateToken(String newToken) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("tokens");
        Token token = new Token(newToken);
        reference.child(Objects.requireNonNull(firebaseUser).getUid()).setValue(token);
    }


    private void sendRegistrationToServer(String token) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            StringRequest request = new StringRequest(
                    Request.Method.GET, API_SET_USER_TOKEN +
                    "?token=" + token +
                    "&email=" + firebaseAuth.getCurrentUser().getEmail(),
                    response -> Log.println(Log.INFO, "New Token Response: ", response),
                    error -> Toast.makeText(this, "Cannot set user token!", Toast.LENGTH_SHORT).show());
            requestQueue.add(request);
        }
    }

    private void sendOreoNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(Objects.requireNonNull(user).replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        OreoNotification oreoNotification = new OreoNotification(this);
        Notification.Builder builder = oreoNotification.getOreoNotification(title, body, pendingIntent, defaultSound, icon);

        int i = 0;
        if (j > 0) {
            i = j;
        }

        oreoNotification.getNotificationManager().notify(i, builder.build());
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(Objects.requireNonNull(user).replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("userid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int i = 0;
        if (j > 0) {
            i = j;
        }

        notificationManager.notify(i, builder.build());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(
                    OreoNotification.CHANNEL_ID,
                    OreoNotification.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT));

            Notification notification = new Notification.Builder(this, OreoNotification.CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("")
                    .build();

            startForeground(1, notification);
        }
    }
}
