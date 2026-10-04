package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class EditorActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private ArrayList<String> photos;
    private Bitmap originalBitmap = null;
    private String currentFilter = "original"; // "original", "pink", "bw"
    private int beautyProgress = 65;

    private Button btnFilterPink;
    private Button btnFilterBW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        imgPreview = findViewById(R.id.imgPreview);
        btnFilterPink = findViewById(R.id.btnFilterPink);
        btnFilterBW = findViewById(R.id.btnFilterBW);
        SeekBar sbBeauty = findViewById(R.id.sbBeauty);
        Button btnProceedFrame = findViewById(R.id.btnProceedFrame);

        photos = getIntent().getStringArrayListExtra("PHOTOS");

        if (photos != null && !photos.isEmpty()) {
            originalBitmap = loadBitmap(photos.get(0));
            applyCurrentFilter();
        } else {
            Toast.makeText(this, "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show();
        }

        if (btnFilterPink != null) {
            btnFilterPink.setOnClickListener(v -> {
                if ("pink".equals(currentFilter)) {
                    currentFilter = "original";
                } else {
                    currentFilter = "pink";
                }
                updateFilterButtonsUI();
                applyCurrentFilter();
            });
        }

        if (btnFilterBW != null) {
            btnFilterBW.setOnClickListener(v -> {
                if ("bw".equals(currentFilter)) {
                    currentFilter = "original";
                } else {
                    currentFilter = "bw";
                }
                updateFilterButtonsUI();
                applyCurrentFilter();
            });
        }

        if (sbBeauty != null) {
            sbBeauty.setProgress(beautyProgress);
            sbBeauty.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    beautyProgress = progress;
                    applyCurrentFilter();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        btnProceedFrame.setOnClickListener(v -> {
            Intent intent = new Intent(EditorActivity.this, PhotostripActivity.class);
            intent.putStringArrayListExtra("PHOTOS", photos);
            intent.putExtra("FILTER_MODE", currentFilter);
            intent.putExtra("BEAUTY_LEVEL", beautyProgress);
            startActivity(intent);
        });
    }

    private void updateFilterButtonsUI() {
        if ("pink".equals(currentFilter)) {
            if (btnFilterPink != null) btnFilterPink.setBackgroundColor(Color.parseColor("#867AFB"));
            if (btnFilterBW != null) btnFilterBW.setBackgroundColor(Color.parseColor("#E0E0E0"));
        } else if ("bw".equals(currentFilter)) {
            if (btnFilterPink != null) btnFilterPink.setBackgroundColor(Color.parseColor("#E0E0E0"));
            if (btnFilterBW != null) btnFilterBW.setBackgroundColor(Color.parseColor("#867AFB"));
        } else {
            if (btnFilterPink != null) btnFilterPink.setBackgroundColor(Color.parseColor("#E0E0E0"));
            if (btnFilterBW != null) btnFilterBW.setBackgroundColor(Color.parseColor("#E0E0E0"));
        }
    }

    private void applyCurrentFilter() {
        if (originalBitmap == null) return;

        Bitmap filtered = applyFilterToBitmap(originalBitmap, currentFilter, beautyProgress);
        imgPreview.setImageBitmap(filtered);
    }

    public static Bitmap applyFilterToBitmap(Bitmap src, String filter, int beautyLevel) {
        if (src == null) return null;

        Bitmap result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorMatrix cm = new ColorMatrix();

        if ("bw".equalsIgnoreCase(filter)) {
            // Black & White (Grayscale)
            cm.setSaturation(0f);
            // Áp dụng thêm độ tương phản và độ sáng theo slider
            float scale = 1f + ((beautyLevel - 50) / 100f) * 0.3f;
            ColorMatrix brightCm = new ColorMatrix(new float[]{
                    scale, 0, 0, 0, (beautyLevel - 50) * 0.5f,
                    0, scale, 0, 0, (beautyLevel - 50) * 0.5f,
                    0, 0, scale, 0, (beautyLevel - 50) * 0.5f,
                    0, 0, 0, 1, 0
            });
            cm.postConcat(brightCm);
        } else if ("pink".equalsIgnoreCase(filter)) {
            // Korean Pink: Tông hồng ấm, tăng sáng nhẹ làn da
            float brightness = (beautyLevel - 50) * 0.6f;
            ColorMatrix pinkMatrix = new ColorMatrix(new float[]{
                    1.12f, 0, 0, 0, 18 + brightness, // Kênh đỏ ấm hơn
                    0, 1.02f, 0, 0, 8 + brightness,  // Kênh xanh lá tự nhiên
                    0, 0, 1.08f, 0, 14 + brightness, // Kênh xanh dương pha tím hồng
                    0, 0, 0, 1, 0
            });
            cm.postConcat(pinkMatrix);
        } else {
            // Original kèm thanh sáng Beauty
            float brightness = (beautyLevel - 50) * 0.5f;
            if (brightness != 0) {
                ColorMatrix brightCm = new ColorMatrix(new float[]{
                        1, 0, 0, 0, brightness,
                        0, 1, 0, 0, brightness,
                        0, 0, 1, 0, brightness,
                        0, 0, 0, 1, 0
                });
                cm.postConcat(brightCm);
            }
        }

        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return result;
    }

    private Bitmap loadBitmap(String pathOrB64) {
        if (pathOrB64 == null || pathOrB64.isEmpty()) return null;
        try {
            File file = new File(pathOrB64);
            if (file.exists() && file.isFile()) {
                return BitmapFactory.decodeFile(pathOrB64);
            }
            String pureBase64 = pathOrB64.contains(",") ? pathOrB64.split(",")[1] : pathOrB64;
            byte[] bytes = Base64.decode(pureBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
