package com.app.motolife.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.app.motolife.MapActivity;
import com.example.motolife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import static com.app.motolife.URI.API.API_SET_USER_TOKEN;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.println(Log.INFO, "From: ", remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.println(Log.INFO, "Message data payload: ", remoteMessage.getData().toString());
            scheduleJob();
        }

        if (remoteMessage.getNotification() != null) {
            Log.println(Log.INFO, "Message Notification Body: ", remoteMessage.getNotification().getBody());
        }

        sendNotification(Objects.requireNonNull(remoteMessage.getNotification()).getTitle(),
                Objects.requireNonNull(remoteMessage.getNotification()).getBody());
    }

    @Override
    public void onNewToken(String token) {
        Log.println(Log.INFO, "Refreshed token: ", token);
        sendRegistrationToServer(token);
    }

    private void scheduleJob() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
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


    private void sendNotification(String messageTitle, String messageBody) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_focused)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}