package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotostripActivity extends AppCompatActivity {

    private ImageView imgStripResult;
    private ArrayList<String> photos;

    private String lastB64Strip = "";
    private String lastQrUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photostrip);

        imgStripResult = findViewById(R.id.imgStripResult);
        photos = getIntent().getStringArrayListExtra("PHOTOS");

        if (photos != null && !photos.isEmpty()) {
            requestRenderPhotostrip();
        }

        Button btnProceedExport = findViewById(R.id.btnProceedExport);
        btnProceedExport.setOnClickListener(v -> {
            // Mở màn hình PreviewActivity để xem ảnh phóng to trước
            Intent intent = new Intent(PhotostripActivity.this, PreviewActivity.class);
            intent.putExtra("B64_STRIP", lastB64Strip);
            intent.putExtra("QR_URL", lastQrUrl);
            startActivity(intent);
        });
    }

    private void requestRenderPhotostrip() {
        ApiService.StripRequest request = new ApiService.StripRequest(
                photos, "1x4", "korean_pink", "SNAPVIBE 2026", "2026.09.08"
        );

        RetrofitClient.getApiService().createStrip(request).enqueue(new Callback<ApiService.StripResponse>() {
            @Override
            public void onResponse(Call<ApiService.StripResponse> call, Response<ApiService.StripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lastB64Strip = response.body().b64Strip;
                    lastQrUrl = response.body().qrUrl;
                    displayBase64(lastB64Strip);
                }
            }

            @Override
            public void onFailure(Call<ApiService.StripResponse> call, Throwable t) {
                Toast.makeText(PhotostripActivity.this, "Lỗi tạo Photostrip: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayBase64(String b64Str) {
        if (b64Str == null || b64Str.isEmpty()) return;
        String pureBase64 = b64Str.contains(",") ? b64Str.split(",")[1] : b64Str;
        byte[] bytes = Base64.decode(pureBase64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        imgStripResult.setImageBitmap(bitmap);
    }
}
