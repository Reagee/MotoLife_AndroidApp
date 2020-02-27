package com.app.motolife;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.motolife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AppCompatActivity;

import static com.app.motolife.URI.API.API_ADD_USER;

public class RegistrationActivity extends AppCompatActivity {

    private EditText username;
    private EditText email;
    private EditText password;
    private Button registerButton;
    private Button backToLogin;
    private FirebaseAuth firebaseAuth;
    private RequestQueue requestQueue;
    private DatabaseReference reference;
    //    private static final String API_URL = "http://s1.ct8.pl:25500/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        initializeFields();

        registerButton.setOnClickListener(click -> {
            if (validateInputs(Arrays.asList(username, email, password))) {
                registerButton.setError(null);
                String emailVal = email.getText().toString();
                String usernameVal = username.getText().toString();
                String passwordVal = password.getText().toString();

                firebaseAuth.createUserWithEmailAndPassword(emailVal, passwordVal).addOnCompleteListener(
                        RegistrationActivity.this, task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Task unsuccessful :( " + task.getResult(), Toast.LENGTH_SHORT).show();
                                registerButton.setError("Provide proper registration data.");
                            } else {
                                Toast.makeText(getApplicationContext(), "Task successful :)", Toast.LENGTH_SHORT).show();
                                StringRequest request = new StringRequest(
                                        Request.Method.GET, API_ADD_USER +
                                        "?username=" + usernameVal +
                                        "&email=" + emailVal +
                                        "&token=" + firebaseAuth.getUid(),
                                        response -> Log.println(Log.INFO, "RESPONSE", response),
                                        error -> Toast.makeText(getApplicationContext(), "Cannot add new user : " + error, Toast.LENGTH_LONG).show());
                                requestQueue.add(request);

                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                String userId = firebaseUser.getUid();

                                reference = FirebaseDatabase.getInstance().getReference("users").child(userId);
                                HashMap<String, String> userMap = new HashMap<>();
                                userMap.put("id", userId);
                                userMap.put("username", usernameVal);
                                userMap.put("email", emailVal);
                                userMap.put("imageURL", "default");
                                userMap.put("status", "offline");

                                reference.setValue(userMap).addOnCompleteListener(tsk -> {
                                    if (tsk.isSuccessful()) {
                                        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                });
                            }
                        });
                Toast.makeText(getApplicationContext(), "Success registration !", Toast.LENGTH_SHORT).show();
            } else
                registerButton.setError("Provide proper registration data.");
        });

        backToLogin.setOnClickListener(click -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        });

    }

    private void initializeFields() {
        firebaseAuth = FirebaseAuth.getInstance();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        username = findViewById(R.id.username_registration);
        email = findViewById(R.id.email_registration);
        password = findViewById(R.id.password_registration);
        registerButton = findViewById(R.id.register_button);
        backToLogin = findViewById(R.id.back_to_login_button);
    }

    private boolean validateInputs(List<EditText> inputs) {
        registerButton.setError(null);
        AtomicBoolean flag = new AtomicBoolean(true);
        inputs.forEach(input -> {
            if (Objects.equals(input, null) || input.getText().toString().isEmpty())
                flag.set(false);
        });
        return flag.get();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
    }
}
