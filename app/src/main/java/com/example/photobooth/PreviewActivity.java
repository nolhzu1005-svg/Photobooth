package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        ImageView imgFinalStrip = findViewById(R.id.imgFinalStrip);
        TextView btnBackFrame = findViewById(R.id.btnBackFrame);
        Button btnGoToQr = findViewById(R.id.btnGoToQr);

        String b64Strip = getIntent().getStringExtra("B64_STRIP");
        String qrUrl = getIntent().getStringExtra("QR_URL");

        if (b64Strip != null && !b64Strip.isEmpty()) {
            String pureBase64 = b64Strip.contains(",") ? b64Strip.split(",")[1] : b64Strip;
            byte[] bytes = Base64.decode(pureBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imgFinalStrip.setImageBitmap(bitmap);
        }

        btnBackFrame.setOnClickListener(v -> finish());

        // Bấm chuyển sang màn hình QR Code
        btnGoToQr.setOnClickListener(v -> {
            Intent intent = new Intent(PreviewActivity.this, GalleryActivity.class);
            intent.putExtra("QR_URL", qrUrl);
            startActivity(intent);
        });
    }
}
