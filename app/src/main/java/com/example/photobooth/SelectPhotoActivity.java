package com.example.photobooth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class SelectPhotoActivity extends AppCompatActivity {

    private ArrayList<String> photos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_photo);

        photos = getIntent().getStringArrayListExtra("PHOTOS");
        Button btnProceedFilter = findViewById(R.id.btnProceedFilter);

        btnProceedFilter.setOnClickListener(v -> {
            Intent intent = new Intent(SelectPhotoActivity.this, EditorActivity.class);
            intent.putStringArrayListExtra("PHOTOS", photos);
            startActivity(intent);
        });
    }
}
