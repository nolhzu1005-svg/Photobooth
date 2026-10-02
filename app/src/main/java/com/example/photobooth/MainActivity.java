package com.example.photobooth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private int selectedShots = 4; // Mặc định 4 tấm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnShots2 = findViewById(R.id.btnShots2);
        Button btnShots4 = findViewById(R.id.btnShots4);
        Button btnShots6 = findViewById(R.id.btnShots6);
        Button btnStartCamera = findViewById(R.id.btnStartCamera);
        Button btnUploadDevice = findViewById(R.id.btnUploadDevice);

        btnShots2.setOnClickListener(v -> selectedShots = 2);
        btnShots4.setOnClickListener(v -> selectedShots = 4);
        btnShots6.setOnClickListener(v -> selectedShots = 6);

        btnStartCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            intent.putExtra("TARGET_SHOTS", selectedShots);
            startActivity(intent);
        });

        btnUploadDevice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectPhotoActivity.class);
            startActivity(intent);
        });
    }
}
