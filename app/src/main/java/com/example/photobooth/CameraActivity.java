package com.example.photobooth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import java.io.File;
import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView viewFinder;
    private TextView tvCountdown;
    private TextView tvShotIndicator;
    private View flashOverlay;
    private Button btnCaptureShutter;
    private ImageButton btnSwitchCamera;

    private ImageCapture imageCapture;

    // Lưu đường dẫn file ảnh, không lưu Base64 để tránh lỗi quá dữ liệu Intent
    private final ArrayList<String> capturedPhotos = new ArrayList<>();

    private int currentShot = 0;
    private int targetShots = 4;
    private int currentLensFacing = CameraSelector.LENS_FACING_FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        targetShots = getIntent().getIntExtra("TARGET_SHOTS", 4);

        viewFinder = findViewById(R.id.viewFinder);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvShotIndicator = findViewById(R.id.tvShotIndicator);
        flashOverlay = findViewById(R.id.flashOverlay);
        btnCaptureShutter = findViewById(R.id.btnCaptureShutter);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);

        btnCaptureShutter.setEnabled(false);

        btnCaptureShutter.setOnClickListener(v -> startShotSequence());

        if (btnSwitchCamera != null) {
            btnSwitchCamera.setOnClickListener(v -> {
                currentLensFacing =
                        currentLensFacing == CameraSelector.LENS_FACING_FRONT
                                ? CameraSelector.LENS_FACING_BACK
                                : CameraSelector.LENS_FACING_FRONT;

                startCamera();
            });
        }

        requestCameraPermissionOrStart();
    }

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

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

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

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                viewFinder.setImplementationMode(
                        PreviewView.ImplementationMode.COMPATIBLE
                );

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector primarySelector =
                        currentLensFacing == CameraSelector.LENS_FACING_FRONT
                                ? CameraSelector.DEFAULT_FRONT_CAMERA
                                : CameraSelector.DEFAULT_BACK_CAMERA;

                CameraSelector fallbackSelector =
                        currentLensFacing == CameraSelector.LENS_FACING_FRONT
                                ? CameraSelector.DEFAULT_BACK_CAMERA
                                : CameraSelector.DEFAULT_FRONT_CAMERA;

                CameraSelector cameraSelector;

                if (cameraProvider.hasCamera(primarySelector)) {
                    cameraSelector = primarySelector;
                } else if (cameraProvider.hasCamera(fallbackSelector)) {
                    cameraSelector = fallbackSelector;
                } else {
                    btnCaptureShutter.setEnabled(false);

                    Toast.makeText(
                            CameraActivity.this,
                            "Không tìm thấy camera. Kiểm tra lại Webcam trong Device Manager.",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        CameraActivity.this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

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

    private void startShotSequence() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCaptureShutter.setEnabled(false);

        capturedPhotos.clear();
        currentShot = 0;

        takeNextShot();
    }

    private void takeNextShot() {
        // Đủ 4 ảnh: chuyển sang trang chọn ảnh
        if (currentShot >= targetShots) {
            Intent intent = new Intent(
                    CameraActivity.this,
                    SelectPhotoActivity.class
            );

            intent.putStringArrayListExtra("PHOTOS", capturedPhotos);
            startActivity(intent);
            finish();
            return;
        }

        currentShot++;

        tvShotIndicator.setText(
                "CHỤP " + currentShot + " / " + targetShots
        );

        tvCountdown.setVisibility(View.VISIBLE);

        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000) + 1;
                tvCountdown.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                tvCountdown.setVisibility(View.GONE);

                if (flashOverlay != null) {
                    flashOverlay.setAlpha(0.95f);
                    flashOverlay.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .start();
                }

                capturePhoto();
            }
        }.start();
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            return;
        }

        File photoFile = new File(
                getExternalFilesDir(null),
                "shot_" + System.currentTimeMillis() + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults
                    ) {
                        // Chỉ lưu đường dẫn file ảnh: dữ liệu rất nhỏ và ổn định.
                        capturedPhotos.add(photoFile.getAbsolutePath());

                        // Chụp tiếp hoặc tự chuyển SelectPhotoActivity khi đủ 4 ảnh.
                        takeNextShot();
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception
                    ) {
                        btnCaptureShutter.setEnabled(true);

                        Toast.makeText(
                                CameraActivity.this,
                                "Chụp ảnh thất bại: " + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}