package com.example.photobooth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotostripActivity extends AppCompatActivity {

    private ImageView imgStripResult;
    private ArrayList<String> photos;

    private TextView stkStar;
    private TextView stkHeart;
    private TextView stkSmile;
    private TextView stkCloud;

    private String selectedSticker = "";
    private String filterMode = "original";
    private int beautyLevel = 65;

    private String lastStripPath = "";
    private String lastB64Strip = "";
    private String lastQrUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photostrip);

        imgStripResult = findViewById(R.id.imgStripResult);
        photos = getIntent().getStringArrayListExtra("PHOTOS");
        filterMode = getIntent().getStringExtra("FILTER_MODE");
        if (filterMode == null) filterMode = "original";
        beautyLevel = getIntent().getIntExtra("BEAUTY_LEVEL", 65);

        stkStar = findViewById(R.id.stkStar);
        stkHeart = findViewById(R.id.stkHeart);
        stkSmile = findViewById(R.id.stkSmile);
        stkCloud = findViewById(R.id.stkCloud);

        setupStickerClickListeners();

        // Render dải photostrip ngay lập tức bằng Canvas local
        if (photos != null && !photos.isEmpty()) {
            renderLocalPhotostrip();
            // Gọi Backend Flask nếu có
            requestRenderPhotostrip();
        } else {
            Toast.makeText(this, "Không có ảnh để tạo dải ảnh", Toast.LENGTH_SHORT).show();
        }

        Button btnProceedExport = findViewById(R.id.btnProceedExport);
        btnProceedExport.setOnClickListener(v -> {
            if (lastStripPath == null || lastStripPath.isEmpty()) {
                Toast.makeText(this, "Đang xử lý dải ảnh, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(PhotostripActivity.this, PreviewActivity.class);
            // Truyền đường dẫn file an toàn tuyệt đối tránh lỗi Binder TransactionTooLargeException
            intent.putExtra("STRIP_PATH", lastStripPath);
            intent.putExtra("QR_URL", lastQrUrl);
            intent.putStringArrayListExtra("PHOTOS", photos);
            intent.putExtra("FILTER_MODE", filterMode);
            intent.putExtra("BEAUTY_LEVEL", beautyLevel);
            startActivity(intent);
        });
    }

    private void setupStickerClickListeners() {
        TextView[] stickers = new TextView[]{stkStar, stkHeart, stkSmile, stkCloud};
        String[] stickerTexts = new String[]{"✦", "♥", "☺", "☁"};

        for (int i = 0; i < stickers.length; i++) {
            if (stickers[i] == null) continue;
            final String symbol = stickerTexts[i];
            final TextView currentView = stickers[i];

            currentView.setOnClickListener(v -> {
                if (selectedSticker.equals(symbol)) {
                    selectedSticker = ""; // Bỏ chọn nếu bấm lại
                } else {
                    selectedSticker = symbol;
                }

                // Cập nhật giao diện nút bấm bo góc đẹp mắt
                for (TextView tv : stickers) {
                    if (tv != null) {
                        tv.setBackgroundResource(R.drawable.bg_white_card);
                        tv.setTextColor(Color.parseColor("#1E1B2E"));
                    }
                }

                if (!selectedSticker.isEmpty()) {
                    currentView.setBackgroundResource(R.drawable.bg_pill_active);
                    currentView.setTextColor(Color.WHITE);
                }

                // Render lại Photostrip với Sticker mới
                renderLocalPhotostrip();
            });
        }
    }

    private void renderLocalPhotostrip() {
        if (photos == null || photos.isEmpty()) return;

        Bitmap bitmap = generateLocalPhotostrip(photos, selectedSticker);
        if (bitmap != null) {
            imgStripResult.setImageBitmap(bitmap);

            // Lưu dải ảnh ra file nội bộ để PreviewActivity và GalleryActivity sử dụng
            try {
                File dir = getExternalFilesDir(null);
                if (dir == null) dir = getFilesDir();
                File stripFile = new File(dir, "photostrip_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream fos = new FileOutputStream(stripFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                    fos.flush();
                }
                lastStripPath = stripFile.getAbsolutePath();

                // Tạo Base64 nhẹ cho thumbnail hoặc QR nếu cần
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                lastB64Strip = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap generateLocalPhotostrip(ArrayList<String> photoPathsOrB64, String stickerSymbol) {
        if (photoPathsOrB64 == null || photoPathsOrB64.isEmpty()) return null;

        int photoWidth = 600;
        int photoHeight = 450;
        int padding = 30;
        int headerHeight = 120;
        int footerHeight = 180;

        int count = photoPathsOrB64.size();
        int totalWidth = photoWidth + (padding * 2);
        int totalHeight = headerHeight + (count * photoHeight) + ((count + 1) * padding) + footerHeight;

        Bitmap stripBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(stripBitmap);

        // Nền hồng phong cách Korean Pink
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#FFD1DC"));
        canvas.drawRect(0, 0, totalWidth, totalHeight, bgPaint);

        // Tiêu đề đầu dải ảnh
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#1E1B2E"));
        textPaint.setTextSize(36f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SNAPVIBE 2026", totalWidth / 2f, 75, textPaint);

        // Vẽ từng bức ảnh
        int currentY = headerHeight;
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);

        for (int i = 0; i < count; i++) {
            String rawPhoto = photoPathsOrB64.get(i);
            Bitmap rawBmp = loadBitmap(rawPhoto);

            if (rawBmp != null) {
                // Áp dụng bộ lọc đã chọn ở bước trước
                Bitmap filteredBmp = EditorActivity.applyFilterToBitmap(rawBmp, filterMode, beautyLevel);
                if (filteredBmp == null) filteredBmp = rawBmp;

                // Cắt ảnh theo tỉ lệ vừa khung (Center Crop)
                Rect srcRect = calculateCenterCropRect(filteredBmp.getWidth(), filteredBmp.getHeight(), photoWidth, photoHeight);
                Rect dstRect = new Rect(padding, currentY, padding + photoWidth, currentY + photoHeight);

                // Viền trắng Polaroid bo góc nhẹ quanh ảnh
                canvas.drawRect(padding - 6, currentY - 6, padding + photoWidth + 6, currentY + photoHeight + 6, borderPaint);
                canvas.drawBitmap(filteredBmp, srcRect, dstRect, null);

                // Nếu có sticker: Thêm góc trang trí sticker trên mỗi tấm ảnh
                if (stickerSymbol != null && !stickerSymbol.isEmpty()) {
                    drawCornerStickerBadge(canvas, stickerSymbol, padding + photoWidth - 40, currentY + 40);
                }

                if (filteredBmp != rawBmp) {
                    filteredBmp.recycle();
                }
                rawBmp.recycle();
            }

            currentY += photoHeight + padding;
        }

        // Vẽ Sticker nổi bật trong phần Footer
        if (stickerSymbol != null && !stickerSymbol.isEmpty()) {
            // Vẽ huy hiệu Sticker dễ thương hình tròn
            Paint badgeCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            badgeCirclePaint.setColor(Color.WHITE);
            float badgeX = totalWidth / 2f;
            float badgeY = currentY + 45;
            canvas.drawCircle(badgeX, badgeY, 35, badgeCirclePaint);

            Paint stickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            stickerPaint.setTextSize(40f);
            stickerPaint.setColor(Color.parseColor("#867AFB"));
            stickerPaint.setTypeface(Typeface.DEFAULT_BOLD);
            stickerPaint.setTextAlign(Paint.Align.CENTER);

            // Điều chỉnh vị trí Y để chữ nằm chính giữa hình tròn
            Paint.FontMetrics fm = stickerPaint.getFontMetrics();
            float textY = badgeY - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(stickerSymbol, badgeX, textY, stickerPaint);

            // Hai ngôi sao lấp lánh hai bên huy hiệu
            Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sparklePaint.setTextSize(22f);
            sparklePaint.setColor(Color.parseColor("#867AFB"));
            sparklePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("✦", badgeX - 60, textY - 2, sparklePaint);
            canvas.drawText("✦", badgeX + 60, textY - 2, sparklePaint);
        }

        // Ngày tháng năm thực tế
        String dateText = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(new Date());
        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setColor(Color.parseColor("#767389"));
        datePaint.setTextSize(24f);
        datePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(dateText, totalWidth / 2f, currentY + 115, datePaint);

        return stripBitmap;
    }

    private void drawCornerStickerBadge(Canvas canvas, String sticker, float cx, float cy) {
        // Nền tròn trắng nhỏ ở góc ảnh
        Paint circleBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleBg.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, 22, circleBg);

        // Icon sticker
        Paint stickerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stickerPaint.setTextSize(24f);
        stickerPaint.setColor(Color.parseColor("#867AFB"));
        stickerPaint.setTypeface(Typeface.DEFAULT_BOLD);
        stickerPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = stickerPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(sticker, cx, textY, stickerPaint);
    }

    private Rect calculateCenterCropRect(int srcW, int srcH, int dstW, int dstH) {
        float srcRatio = (float) srcW / srcH;
        float dstRatio = (float) dstW / dstH;

        int cropW, cropH;
        if (srcRatio > dstRatio) {
            cropH = srcH;
            cropW = (int) (srcH * dstRatio);
        } else {
            cropW = srcW;
            cropH = (int) (srcW / dstRatio);
        }

        int xOffset = (srcW - cropW) / 2;
        int yOffset = (srcH - cropH) / 2;
        return new Rect(xOffset, yOffset, xOffset + cropW, yOffset + cropH);
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

    private void requestRenderPhotostrip() {
        // Tạo chuỗi base64 cho các ảnh để gửi server nếu cần
        new Thread(() -> {
            try {
                ArrayList<String> b64List = new ArrayList<>();
                for (String p : photos) {
                    Bitmap bmp = loadBitmap(p);
                    if (bmp != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        b64List.add("data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP));
                        bmp.recycle();
                    }
                }

                ApiService.StripRequest request = new ApiService.StripRequest(
                        b64List, "1x4", "korean_pink", "SNAPVIBE 2026", "2026.09.08"
                );

                RetrofitClient.getApiService().createStrip(request).enqueue(new Callback<ApiService.StripResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.StripResponse> call, Response<ApiService.StripResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().qrUrl != null) {
                                lastQrUrl = response.body().qrUrl;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.StripResponse> call, Throwable t) {
                        // Canvas local đã vẽ hoàn hảo, không cần phụ thuộc vào server
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
