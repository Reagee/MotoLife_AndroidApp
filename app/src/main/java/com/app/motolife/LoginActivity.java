package com.app.motolife;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.app.motolife.URI.PowerOffController;
import com.example.motolife.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private Button loginButton;
    private Button goToRegisterButton;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private final boolean[] exitAppFlag = new boolean[]{false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email_login);
        password = findViewById(R.id.password_login);
        loginButton = findViewById(R.id.login_button);
        goToRegisterButton = findViewById(R.id.go_to_register);

        loginButton.setOnClickListener(click -> {
            if (validateInputs(Arrays.asList(email, password))) {
                loginButton.setError(null);
                String emailVal = email.getText().toString();
                String passwordVal = password.getText().toString();

                auth.signInWithEmailAndPassword(emailVal, passwordVal).addOnCompleteListener(
                        LoginActivity.this, task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Task unsuccessful :(" + task.getResult(), Toast.LENGTH_LONG).show();
                                loginButton.setError("Provide proper registration data.");
                            } else {
                                Toast.makeText(getApplicationContext(), "Task successful :)", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MapActivity.class));
                            }
                        });
                Toast.makeText(getApplicationContext(), "Success login !", Toast.LENGTH_SHORT).show();
            } else
                loginButton.setError("Provide proper registration data.");
        });

        goToRegisterButton.setOnClickListener(click -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        });
    }

    private boolean validateInputs(List<EditText> inputs) {
        loginButton.setError(null);
        AtomicBoolean flag = new AtomicBoolean(true);
        inputs.forEach(input -> {
            if (Objects.equals(input, null)) flag.set(false);
        });
        return flag.get();
    }

    @Override
    public void onBackPressed() {
        PowerOffController.powerOff(exitAppFlag,this);
    }
}
