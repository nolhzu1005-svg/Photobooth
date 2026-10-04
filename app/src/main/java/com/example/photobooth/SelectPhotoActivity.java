package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SelectPhotoActivity extends AppCompatActivity {

    private ArrayList<String> photos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_photo);

        photos = getIntent().getStringArrayListExtra("PHOTOS");

        if (photos == null || photos.isEmpty()) {
            photos = new ArrayList<>();
            // Tự động tìm lại các ảnh đã chụp gần nhất trên máy nếu vào từ nút "Tải ảnh từ thiết bị"
            File dir = getExternalFilesDir(null);
            if (dir != null) {
                File[] files = dir.listFiles((d, name) -> name.startsWith("shot_") && name.endsWith(".jpg"));
                if (files != null && files.length > 0) {
                    Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                    for (int i = 0; i < Math.min(4, files.length); i++) {
                        photos.add(files[i].getAbsolutePath());
                    }
                }
            }
        }

        GridView gvPhotos = findViewById(R.id.gvPhotos);
        TextView tvCountInfo = findViewById(R.id.tvCountInfo);
        Button btnProceedFilter = findViewById(R.id.btnProceedFilter);
        Button btnRetake = findViewById(R.id.btnRetake);

        tvCountInfo.setText(photos.size() + " ảnh đã sẵn sàng");

        gvPhotos.setAdapter(new PhotoGridAdapter());

        btnProceedFilter.setOnClickListener(v -> {
            if (photos.isEmpty()) {
                tvCountInfo.setText("Chưa có ảnh. Hãy chụp lại.");
                return;
            }

            Intent intent = new Intent(
                    SelectPhotoActivity.this,
                    EditorActivity.class
            );

            intent.putStringArrayListExtra("PHOTOS", photos);
            startActivity(intent);
        });

        if (btnRetake != null) {
            btnRetake.setOnClickListener(v -> {
                Intent intent = new Intent(
                        SelectPhotoActivity.this,
                        CameraActivity.class
                );

                startActivity(intent);
                finish();
            });
        }
    }

    private class PhotoGridAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object getItem(int position) {
            return photos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            if (convertView == null) {
                imageView = new ImageView(SelectPhotoActivity.this);

                imageView.setLayoutParams(
                        new GridView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                400
                        )
                );

                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
                imageView.setBackgroundColor(Color.LTGRAY);
            } else {
                imageView = (ImageView) convertView;
            }

            String imagePath = photos.get(position);

            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageBitmap(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageBitmap(null);
            }

            return imageView;
        }
    }
}