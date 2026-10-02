package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class EditorActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private ArrayList<String> photos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        imgPreview = findViewById(R.id.imgPreview);
        photos = getIntent().getStringArrayListExtra("PHOTOS");

        if (photos != null && !photos.isEmpty()) {
            displayBase64(photos.get(0));
        }

        Button btnProceedFrame = findViewById(R.id.btnProceedFrame);
        btnProceedFrame.setOnClickListener(v -> {
            Intent intent = new Intent(EditorActivity.this, PhotostripActivity.class);
            intent.putStringArrayListExtra("PHOTOS", photos);
            startActivity(intent);
        });
    }

    private void displayBase64(String b64Str) {
        String pureBase64 = b64Str.contains(",") ? b64Str.split(",")[1] : b64Str;
        byte[] bytes = Base64.decode(pureBase64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imgPreview.setImageBitmap(bitmap);
    }
}
