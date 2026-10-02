package com.example.photobooth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView viewFinder;
    private TextView tvCountdown;
    private TextView tvShotIndicator;
    private View flashOverlay;
    private Button btnCaptureShutter;

    private ImageCapture imageCapture;

    private final ArrayList<String> capturedPhotos = new ArrayList<>();

    private int currentShot = 0;
    private int targetShots = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        // Nhận số lượng ảnh cần chụp
        targetShots = getIntent().getIntExtra("TARGET_SHOTS", 4);

        // Ánh xạ View
        viewFinder = findViewById(R.id.viewFinder);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvShotIndicator = findViewById(R.id.tvShotIndicator);
        flashOverlay = findViewById(R.id.flashOverlay);
        btnCaptureShutter = findViewById(R.id.btnCaptureShutter);

        // Ban đầu chưa cho bấm nút chụp
        btnCaptureShutter.setEnabled(false);

        // Bấm nút chụp
        btnCaptureShutter.setOnClickListener(v -> startShotSequence());

        // Kiểm tra quyền camera
        requestCameraPermissionOrStart();
    }

    /**
     * Kiểm tra quyền Camera
     */
    private void requestCameraPermissionOrStart() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Nhận kết quả cấp quyền Camera
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            Toast.makeText(
                    this,
                    "Bạn cần cấp quyền Camera để chụp ảnh",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Khởi động CameraX
     */
    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {

            try {

                ProcessCameraProvider cameraProvider =
                        cameraProviderFuture.get();

                // ==============================
                // PREVIEW
                // ==============================

                Preview preview =
                        new Preview.Builder().build();

                preview.setSurfaceProvider(
                        viewFinder.getSurfaceProvider()
                );

                // ==============================
                // IMAGE CAPTURE
                // ==============================

                imageCapture =
                        new ImageCapture.Builder().build();

                // ==============================
                // CHỌN CAMERA
                // ==============================

                CameraSelector cameraSelector;

                /*
                 * Ưu tiên camera trước.
                 *
                 * Nếu Emulator không có camera trước
                 * thì thử camera sau.
                 */

                if (cameraProvider.hasCamera(
                        CameraSelector.DEFAULT_FRONT_CAMERA)) {

                    cameraSelector =
                            CameraSelector.DEFAULT_FRONT_CAMERA;

                } else if (cameraProvider.hasCamera(
                        CameraSelector.DEFAULT_BACK_CAMERA)) {

                    cameraSelector =
                            CameraSelector.DEFAULT_BACK_CAMERA;

                } else {

                    btnCaptureShutter.setEnabled(false);

                    Toast.makeText(
                            CameraActivity.this,
                            "Không tìm thấy camera trên Emulator. " +
                                    "Hãy kiểm tra Webcam trong Device Manager.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                // Hủy camera cũ
                cameraProvider.unbindAll();

                // Kết nối CameraX
                cameraProvider.bindToLifecycle(
                        CameraActivity.this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

                // Camera đã sẵn sàng
                btnCaptureShutter.setEnabled(true);

                Toast.makeText(
                        CameraActivity.this,
                        "Camera đã sẵn sàng",
                        Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {

                btnCaptureShutter.setEnabled(false);

                Toast.makeText(
                        CameraActivity.this,
                        "Không thể mở camera: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Bắt đầu quá trình chụp nhiều ảnh
     */
    private void startShotSequence() {

        // Kiểm tra CameraX
        if (imageCapture == null) {

            Toast.makeText(
                    this,
                    "Camera chưa sẵn sàng",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Không cho bấm nút trong lúc đang chụp
        btnCaptureShutter.setEnabled(false);

        // Xóa danh sách ảnh cũ
        capturedPhotos.clear();

        // Đặt lại số thứ tự ảnh
        currentShot = 0;

        // Bắt đầu chụp ảnh đầu tiên
        takeNextShot();
    }

    /**
     * Chụp từng ảnh một
     */
    private void takeNextShot() {

        // Nếu đã chụp đủ số lượng ảnh
        if (currentShot >= targetShots) {

            Intent intent = new Intent(
                    CameraActivity.this,
                    SelectPhotoActivity.class
            );

            intent.putStringArrayListExtra(
                    "PHOTOS",
                    capturedPhotos
            );

            startActivity(intent);

            finish();

            return;
        }

        // Tăng số thứ tự ảnh
        currentShot++;

        // Hiển thị số ảnh
        tvShotIndicator.setText(
                "CHỤP " + currentShot + " / " + targetShots
        );

        // Hiển thị đếm ngược
        tvCountdown.setVisibility(View.VISIBLE);

        // Đếm ngược 3 giây
        new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                int seconds =
                        (int) (millisUntilFinished / 1000) + 1;

                tvCountdown.setText(
                        String.valueOf(seconds)
                );
            }

            @Override
            public void onFinish() {

                tvCountdown.setVisibility(View.GONE);

                // Hiệu ứng flash
                if (flashOverlay != null) {

                    flashOverlay.setAlpha(0.95f);

                    flashOverlay
                            .animate()
                            .alpha(0f)
                            .setDuration(200)
                            .start();
                }

                // Chụp ảnh
                capturePhoto();
            }

        }.start();
    }

    /**
     * Chụp và lưu ảnh
     */
    private void capturePhoto() {

        if (imageCapture == null) {
            return;
        }

        // Tạo file ảnh
        File photoFile = new File(
                getExternalFilesDir(null),
                "shot_" +
                        System.currentTimeMillis() +
                        ".jpg"
        );

        // Cấu hình nơi lưu ảnh
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        photoFile
                ).build();

        // Tiến hành chụp
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),

                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults
                    ) {

                        // Đọc ảnh vừa chụp
                        Bitmap bitmap =
                                BitmapFactory.decodeFile(
                                        photoFile.getAbsolutePath()
                                );

                        if (bitmap == null) {

                            btnCaptureShutter.setEnabled(true);

                            Toast.makeText(
                                    CameraActivity.this,
                                    "Không đọc được ảnh vừa chụp",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // Chuyển Bitmap thành byte[]
                        ByteArrayOutputStream baos =
                                new ByteArrayOutputStream();

                        bitmap.compress(
                                Bitmap.CompressFormat.JPEG,
                                85,
                                baos
                        );

                        // Chuyển thành Base64
                        String b64 =
                                Base64.encodeToString(
                                        baos.toByteArray(),
                                        Base64.NO_WRAP
                                );

                        // Lưu ảnh vào danh sách
                        capturedPhotos.add(
                                "data:image/jpeg;base64," + b64
                        );

                        // Giải phóng Bitmap
                        bitmap.recycle();

                        // Chụp ảnh tiếp theo
                        takeNextShot();
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception
                    ) {

                        btnCaptureShutter.setEnabled(true);

                        Toast.makeText(
                                CameraActivity.this,
                                "Chụp ảnh thất bại: " +
                                        exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}