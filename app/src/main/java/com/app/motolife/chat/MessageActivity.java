package com.app.motolife.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.app.motolife.Notifications.Client;
import com.app.motolife.Notifications.Data;
import com.app.motolife.Notifications.MyResponse;
import com.app.motolife.Notifications.Sender;
import com.app.motolife.Notifications.Token;
import com.app.motolife.firebase.APIService;
import com.app.motolife.firebase.UserStatus;
import com.app.motolife.model.Chat;
import com.app.motolife.model.User;
import com.bumptech.glide.Glide;
import com.example.motolife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    private CircleImageView profile_image;
    private TextView username;

    private FirebaseUser firebaseUser;
    private DatabaseReference reference;

    private ImageButton send_button;
    private EditText message_to_send;

    private MessageAdapter messageAdapter;
    private List<Chat> mChat;

    private RecyclerView recyclerView;

    private ValueEventListener seenListener;

    private String userid;
    private APIService apiService;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(click -> finish());

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.user_image);
        username = findViewById(R.id.username);
        send_button = findViewById(R.id.send_message_button);
        message_to_send = findViewById(R.id.message_to_send);

        Intent intent = getIntent();
        userid = intent.getStringExtra("userid");

        send_button.setOnClickListener(click -> {
            notify = true;
            String msg = message_to_send.getText().toString();
            if (!Objects.equals(message_to_send.getText().toString(), "")) {
                sendMessage(firebaseUser.getUid(), userid, msg);
                message_to_send.setText(null);
            } else
                Toast.makeText(this, "You can't send empty message", Toast.LENGTH_SHORT).show();
        });

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(Objects.requireNonNull(user).getUsername());
                if (user.getImageURL().equals("default")) {
                    profile_image.setImageResource(R.drawable.ic_child_care_black_24dp);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
                readMessages(firebaseUser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        seenMessage(userid);

    }

    private void seenMessage(String userid) {
        reference = FirebaseDatabase.getInstance().getReference("chat");
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (Objects.requireNonNull(chat).getReceiver().equals(firebaseUser.getUid()) &&
                            chat.getSender().equals(userid)) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("isseen", true);
                        snapshot.getRef().updateChildren(map);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(String sender, String receiver, String message) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> messageMap = new HashMap<>();
        messageMap.put("sender", sender);
        messageMap.put("receiver", receiver);
        messageMap.put("message", message);
        messageMap.put("isseen", false);

        reference.child("chat").push().setValue(messageMap);

        final String msg = message;
        reference = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify)
                    sendNotification(receiver, Objects.requireNonNull(user).getUsername(), msg);

                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendNotification(String receiver, String username, String message) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("tokens");
        Query query = tokens.orderByKey().equalTo(receiver);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(firebaseUser.getUid(), R.drawable.motolife_logo_icon_dark, username + ": " + message, "New Message!",
                            userid);

                    Sender sender = new Sender(data, Objects.requireNonNull(token).getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(@NotNull Call<MyResponse> call, @NotNull Response<MyResponse> response) {
                                    if (response.code() == 200) {
                                        if (Objects.requireNonNull(response.body()).success != 1) {
                                            Toast.makeText(MessageActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages(String myid, String userid, String imageurl) {
        mChat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("chat");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {
                        mChat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void currentUser(String userid){
        SharedPreferences.Editor editor = getSharedPreferences("PREFS",MODE_PRIVATE).edit();
        editor.putString("currentuser",userid);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserStatus.ONLINE();
        currentUser(userid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        reference.removeEventListener(seenListener);
        currentUser("none");
    }
}
