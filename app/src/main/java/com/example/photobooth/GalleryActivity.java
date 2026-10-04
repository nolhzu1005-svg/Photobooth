package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private final List<File> allStripFiles = new ArrayList<>();
    private final List<File> displayedFiles = new ArrayList<>();
    private GalleryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        GridView gvGallery = findViewById(R.id.gvGallery);
        EditText etSearch = findViewById(R.id.etSearch);
        Button btnNewPhotobooth = findViewById(R.id.btnNewPhotobooth);

        loadSavedStrips();

        adapter = new GalleryAdapter();
        gvGallery.setAdapter(adapter);

        gvGallery.setOnItemClickListener((parent, view, position, id) -> {
            if (position < displayedFiles.size()) {
                File file = displayedFiles.get(position);
                Intent intent = new Intent(GalleryActivity.this, PreviewActivity.class);
                intent.putExtra("STRIP_PATH", file.getAbsolutePath());
                startActivity(intent);
            }
        });

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterStrips(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        btnNewPhotobooth.setOnClickListener(v -> {
            Intent intent = new Intent(GalleryActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadSavedStrips() {
        allStripFiles.clear();
        displayedFiles.clear();

        File dir = getExternalFilesDir(null);
        if (dir == null) dir = getFilesDir();

        File[] files = dir.listFiles((d, name) -> name.startsWith("photostrip_") && name.endsWith(".jpg"));
        if (files != null && files.length > 0) {
            List<File> list = Arrays.asList(files);
            // Sắp xếp ảnh mới nhất lên đầu
            Collections.sort(list, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            allStripFiles.addAll(list);
            displayedFiles.addAll(list);
        }
    }

    private void filterStrips(String query) {
        displayedFiles.clear();
        if (query == null || query.trim().isEmpty()) {
            displayedFiles.addAll(allStripFiles);
        } else {
            String lower = query.toLowerCase();
            for (File f : allStripFiles) {
                if (f.getName().toLowerCase().contains(lower)) {
                    displayedFiles.add(f);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class GalleryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return displayedFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return displayedFiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(GalleryActivity.this);
                imageView.setLayoutParams(new GridView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                ));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setPadding(8, 8, 8, 8);
                imageView.setBackgroundResource(R.drawable.bg_white_card);
            } else {
                imageView = (ImageView) convertView;
            }

            File file = displayedFiles.get(position);
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                imageView.setImageBitmap(null);
            }

            return imageView;
        }
    }
}
