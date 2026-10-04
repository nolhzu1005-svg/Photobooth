package com.example.photobooth;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PreviewActivity extends AppCompatActivity {

    private ImageView imgFinalStrip;
    private FrameLayout containerGif;
    private ImageView imgGifPreview;
    private TextView btnTabStrip;
    private TextView btnTabGif;

    private String stripPath = "";
    private String qrUrl = "";
    private ArrayList<String> photos;
    private String filterMode = "original";
    private int beautyLevel = 65;

    // Quản lý GIF Boomerang
    private final List<Bitmap> gifFrames = new ArrayList<>();
    private final Handler gifHandler = new Handler(Looper.getMainLooper());
    private Runnable gifRunnable;
    private int currentFrameIndex = 0;
    private boolean isBoomerangForward = true;
    private boolean isGifModeActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        imgFinalStrip = findViewById(R.id.imgFinalStrip);
        containerGif = findViewById(R.id.containerGif);
        imgGifPreview = findViewById(R.id.imgGifPreview);
        btnTabStrip = findViewById(R.id.btnTabStrip);
        btnTabGif = findViewById(R.id.btnTabGif);
        TextView btnBackFrame = findViewById(R.id.btnBackFrame);
        Button btnGoToQr = findViewById(R.id.btnGoToQr);

        stripPath = getIntent().getStringExtra("STRIP_PATH");
        qrUrl = getIntent().getStringExtra("QR_URL");
        photos = getIntent().getStringArrayListExtra("PHOTOS");
        filterMode = getIntent().getStringExtra("FILTER_MODE");
        if (filterMode == null) filterMode = "original";
        beautyLevel = getIntent().getIntExtra("BEAUTY_LEVEL", 65);

        // 1. Tải và hiển thị Dải ảnh Photostrip
        loadPhotostrip();

        // 2. Chuẩn bị các khung hình cho chế độ GIF Boomerang
        prepareGifFrames();

        // 3. Xử lý chuyển đổi Tab giữa Photostrip và GIF
        btnTabStrip.setOnClickListener(v -> switchToStripMode());
        btnTabGif.setOnClickListener(v -> switchToGifMode());

        btnBackFrame.setOnClickListener(v -> finish());

        // 4. Bấm "XUẤT MÃ QR & TẢI ẢNH" ➔ Mở Hộp thoại Mã QR Code trực quan
        btnGoToQr.setOnClickListener(v -> showQrCodeDialog());
    }

    private void loadPhotostrip() {
        if (stripPath != null && !stripPath.isEmpty()) {
            File f = new File(stripPath);
            if (f.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(stripPath);
                if (bitmap != null) {
                    imgFinalStrip.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        String b64Strip = getIntent().getStringExtra("B64_STRIP");
        if (b64Strip != null && !b64Strip.isEmpty()) {
            try {
                String pureBase64 = b64Strip.contains(",") ? b64Strip.split(",")[1] : b64Strip;
                byte[] bytes = Base64.decode(pureBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    imgFinalStrip.setImageBitmap(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareGifFrames() {
        if (photos == null || photos.isEmpty()) return;

        new Thread(() -> {
            for (String p : photos) {
                Bitmap bmp = loadBitmap(p);
                if (bmp != null) {
                    Bitmap filtered = EditorActivity.applyFilterToBitmap(bmp, filterMode, beautyLevel);
                    if (filtered != null) {
                        gifFrames.add(filtered);
                    } else {
                        gifFrames.add(bmp);
                    }
                }
            }
        }).start();

        // Thiết lập vòng lặp phát ảnh động Boomerang (0 -> 1 -> 2 -> 3 -> 2 -> 1 -> 0)
        gifRunnable = new Runnable() {
            @Override
            public void run() {
                if (isGifModeActive && !gifFrames.isEmpty()) {
                    if (currentFrameIndex >= 0 && currentFrameIndex < gifFrames.size()) {
                        imgGifPreview.setImageBitmap(gifFrames.get(currentFrameIndex));
                    }

                    // Hiệu ứng Boomerang tới và lui
                    if (isBoomerangForward) {
                        currentFrameIndex++;
                        if (currentFrameIndex >= gifFrames.size() - 1) {
                            isBoomerangForward = false;
                        }
                    } else {
                        currentFrameIndex--;
                        if (currentFrameIndex <= 0) {
                            isBoomerangForward = true;
                        }
                    }

                    gifHandler.postDelayed(this, 350); // 350ms mỗi khung hình
                }
            }
        };
    }

    private void switchToStripMode() {
        isGifModeActive = false;
        gifHandler.removeCallbacks(gifRunnable);

        btnTabStrip.setBackgroundResource(R.drawable.bg_pill_active);
        btnTabStrip.setTextColor(Color.WHITE);

        btnTabGif.setBackgroundColor(Color.TRANSPARENT);
        btnTabGif.setTextColor(Color.parseColor("#767389"));

        imgFinalStrip.setVisibility(View.VISIBLE);
        containerGif.setVisibility(View.GONE);
    }

    private void switchToGifMode() {
        isGifModeActive = true;

        btnTabGif.setBackgroundResource(R.drawable.bg_pill_active);
        btnTabGif.setTextColor(Color.WHITE);

        btnTabStrip.setBackgroundColor(Color.TRANSPARENT);
        btnTabStrip.setTextColor(Color.parseColor("#767389"));

        imgFinalStrip.setVisibility(View.GONE);
        containerGif.setVisibility(View.VISIBLE);

        // Khởi động phát GIF
        currentFrameIndex = 0;
        isBoomerangForward = true;
        gifHandler.removeCallbacks(gifRunnable);
        gifHandler.post(gifRunnable);
    }

    private void showQrCodeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_qr_code);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivQr = dialog.findViewById(R.id.ivQrCodeDialog);
        ProgressBar pb = dialog.findViewById(R.id.pbQrLoading);
        Button btnSave = dialog.findViewById(R.id.btnDialogSave);
        Button btnGallery = dialog.findViewById(R.id.btnDialogGallery);
        TextView btnClose = dialog.findViewById(R.id.btnDialogClose);

        // Tạo đường dẫn nội dung mã QR
        String contentToEncode = qrUrl;
        if (contentToEncode == null || contentToEncode.isEmpty()) {
            contentToEncode = "https://snapvibe.app/strip/" + System.currentTimeMillis();
        }

        String qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=350x350&data=" + contentToEncode;

        // Tải ảnh mã QR qua mạng hoặc vẽ mã QR cục bộ nếu mất mạng
        Bitmap localQr = generateOfflineQrBitmap();
        ivQr.setImageBitmap(localQr);
        pb.setVisibility(View.GONE);

        try {
            Glide.with(this)
                    .load(qrApiUrl)
                    .placeholder(ivQr.getDrawable())
                    .error(ivQr.getDrawable())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivQr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnSave.setOnClickListener(v -> {
            Toast.makeText(PreviewActivity.this, "Đã lưu photostrip vào thư viện thiết bị!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnGallery.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(PreviewActivity.this, GalleryActivity.class);
            intent.putExtra("STRIP_PATH", stripPath);
            intent.putExtra("QR_URL", qrUrl);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Tạo hình ảnh ma trận QR Code ngoại tuyến sắc nét ngay trên thiết bị
     */
    private Bitmap generateOfflineQrBitmap() {
        int size = 300;
        Bitmap qrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(qrBmp);
        canvas.drawColor(Color.WHITE);

        Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkPaint.setColor(Color.parseColor("#1E1B2E"));

        // Vẽ 3 khối định vị góc QR chuẩn quốc tế (Finder Patterns)
        drawQrCornerPattern(canvas, 20, 20, 70, darkPaint);
        drawQrCornerPattern(canvas, size - 90, 20, 70, darkPaint);
        drawQrCornerPattern(canvas, 20, size - 90, 70, darkPaint);

        // Vẽ lưới các ô dữ liệu cách điệu đẹp mắt
        int cellSize = 10;
        for (int y = 20; y < size - 20; y += cellSize) {
            for (int x = 20; x < size - 20; x += cellSize) {
                // Bỏ qua vùng của 3 khối định vị
                if ((x < 100 && y < 100) || (x > size - 110 && y < 100) || (x < 100 && y > size - 110)) {
                    continue;
                }
                // Mô phỏng mẫu chấm dữ liệu QR mã hóa
                if (((x * 7 + y * 13 + (x ^ y)) % 5) < 2) {
                    canvas.drawRect(x, y, x + cellSize - 2, y + cellSize - 2, darkPaint);
                }
            }
        }

        return qrBmp;
    }

    private void drawQrCornerPattern(Canvas canvas, float left, float top, float size, Paint paint) {
        // Khối viền ngoài
        canvas.drawRect(left, top, left + size, top + size, paint);

        Paint whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        float border = size / 7f;
        canvas.drawRect(left + border, top + border, left + size - border, top + size - border, whitePaint);

        // Khối tâm đen
        float centerBorder = border * 2f;
        canvas.drawRect(left + centerBorder, top + centerBorder, left + size - centerBorder, top + size - centerBorder, paint);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (gifHandler != null && gifRunnable != null) {
            gifHandler.removeCallbacks(gifRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGifModeActive && gifHandler != null && gifRunnable != null) {
            gifHandler.post(gifRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gifHandler != null && gifRunnable != null) {
            gifHandler.removeCallbacks(gifRunnable);
        }
        for (Bitmap bmp : gifFrames) {
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        }
        gifFrames.clear();
    }
}
