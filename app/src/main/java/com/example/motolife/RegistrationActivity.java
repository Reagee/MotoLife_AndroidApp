package com.example.motolife;

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
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    private EditText username;
    private EditText email;
    private EditText password;
    private Button registerButton;
    private Button backToLogin;
    private FirebaseAuth firebaseAuth;
    private RequestQueue requestQueue;
    //    private static final String API_URL = "http://s1.ct8.pl:25500/";
    private static final String API_URL = "http://192.168.0.16:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        initializeFields();

        registerButton.setOnClickListener(click -> {
            if (validateInputs(Arrays.asList(username, email, password))) {
                String emailVal = email.getText().toString();
                String usernameVal = username.getText().toString();
                String passwordVal = password.getText().toString();

                firebaseAuth.createUserWithEmailAndPassword(emailVal, passwordVal).addOnCompleteListener(
                        RegistrationActivity.this, task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Task unsuccessful :( " + task.getResult(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Task successful :)", Toast.LENGTH_SHORT).show();
                                StringRequest request = new StringRequest(
                                        Request.Method.GET, API_URL + "addUser" +
                                        "?username=" + usernameVal +
                                        "&email=" + emailVal +
                                        "&token=" + firebaseAuth.getUid(),
                                        response ->
                                                Log.println(Log.INFO, "RESPONSE", response),
                                        error -> {
                                            Toast.makeText(getApplicationContext(), "Cannot update current location : " + error, Toast.LENGTH_LONG).show();
                                        });
                                requestQueue.add(request);
                                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                            }
                        });
                Toast.makeText(getApplicationContext(), "Success registration !", Toast.LENGTH_SHORT).show();
            }
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
}
