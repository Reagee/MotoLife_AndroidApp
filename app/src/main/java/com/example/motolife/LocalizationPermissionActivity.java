package com.example.motolife;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;

import com.example.motolife.R;

public class LocalizationPermissionActivity extends AppCompatActivity {

    private Button permissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_localization_permission);

        permissionButton = findViewById(R.id.permissionButton);
        permissionButton.setOnClickListener(click -> {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            startActivity(new Intent(LocalizationPermissionActivity.this, MapActivity.class));
            permissionButton.setError(null);
        } else {
            permissionButton.setError("You need to get access to your localization.");
        }
    }
}
