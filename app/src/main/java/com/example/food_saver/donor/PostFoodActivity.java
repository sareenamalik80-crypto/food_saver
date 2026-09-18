package com.example.food_saver.donor;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.food_saver.databinding.ActivityPostFoodBinding;
import com.example.food_saver.utils.AiImageChecker;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.utils.ImageUtils;
import com.example.food_saver.utils.LocationHelper;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.Calendar;

/**
 * Lets a donor take a LIVE photo (no gallery picker — CameraX only) of the
 * food, fill in details (including pickup location via GPS), and post it.
 * The photo is compressed and stored as a Base64 string directly in
 * Firestore (no Firebase Storage — that requires the paid Blaze plan).
 */
public class PostFoodActivity extends AppCompatActivity {

    private ActivityPostFoodBinding binding;
    private FoodPostRepository foodPostRepository;

    private ImageCapture imageCapture;
    private Uri capturedPhotoUri;
    private long selectedExpiresAtMillis = -1L;
    private double pickupLat = 0.0;
    private double pickupLng = 0.0;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to post food.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocation();
                } else {
                    Toast.makeText(this, "Location permission is required to use your current location.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        foodPostRepository = new FoodPostRepository();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etExpiryDate.setFocusable(false);
        binding.etExpiryDate.setOnClickListener(v -> showDatePicker());

        binding.btnUseLocation.setOnClickListener(v -> checkLocationPermissionAndFetch());

        binding.btnCapture.setOnClickListener(v -> capturePhoto());
        binding.btnRetake.setOnClickListener(v -> resetToCameraView());
        binding.btnSubmit.setOnClickListener(v -> attemptSubmit());

        checkCameraPermissionAndStart();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLocation() {
        binding.btnUseLocation.setEnabled(false);
        binding.btnUseLocation.setText("Getting location...");

        LocationHelper.fetchCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationFound(double latitude, double longitude, String address) {
                pickupLat = latitude;
                pickupLng = longitude;
                binding.etPickupLocation.setText(
                        address != null ? address
                                : String.format(java.util.Locale.getDefault(), "%.6f, %.6f", latitude, longitude));
                binding.btnUseLocation.setEnabled(true);
                binding.btnUseLocation.setText("📍 Use My Current Location");
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(PostFoodActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                binding.btnUseLocation.setEnabled(true);
                binding.btnUseLocation.setText("📍 Use My Current Location");
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Toast.makeText(this, "Could not start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        // Save to the app's own private cache dir instead of the public
        // MediaStore gallery — the photo is only ever used internally
        // (AI check + upload), never needs to appear in the user's gallery,
        // and this sidesteps MediaStore write failures seen on some OEM
        // ROMs (e.g. "Failed to write to MediaStore URI: null" on Huawei).
        java.io.File photoFile = new java.io.File(getCacheDir(),
                "foodbridge_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        capturedPhotoUri = Uri.fromFile(photoFile);
                        showCapturedPreview();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(PostFoodActivity.this,
                                "Capture failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showCapturedPreview() {
        binding.cameraPreview.setVisibility(View.GONE);
        binding.btnCapture.setVisibility(View.GONE);

        binding.ivCapturedPhoto.setVisibility(View.VISIBLE);
        binding.ivCapturedPhoto.setImageURI(capturedPhotoUri);
        binding.btnRetake.setVisibility(View.VISIBLE);
        binding.detailsForm.setVisibility(View.VISIBLE);
    }

    private void resetToCameraView() {
        capturedPhotoUri = null;
        binding.cameraPreview.setVisibility(View.VISIBLE);
        binding.btnCapture.setVisibility(View.VISIBLE);
        binding.ivCapturedPhoto.setVisibility(View.GONE);
        binding.btnRetake.setVisibility(View.GONE);
        binding.detailsForm.setVisibility(View.GONE);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 23, 59, 59);
            selectedExpiresAtMillis = picked.getTimeInMillis();

            String formatted = String.format(java.util.Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.etExpiryDate.setText(formatted);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        // Food can't expire in the past.
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void attemptSubmit() {
        if (capturedPhotoUri == null) {
            Toast.makeText(this, "Please take a photo of the food first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String donorName = binding.etDonorName.getText().toString().trim();
        String foodName = binding.etFoodTitle.getText().toString().trim();
        String quantity = binding.etQuantity.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String pickupLocation = binding.etPickupLocation.getText().toString().trim();
        String pickupWindow = binding.etPickupWindow.getText().toString().trim();

        if (TextUtils.isEmpty(donorName) || TextUtils.isEmpty(foodName) ||
                TextUtils.isEmpty(quantity) || selectedExpiresAtMillis < 0 ||
                TextUtils.isEmpty(pickupLocation) || TextUtils.isEmpty(pickupWindow)) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Compress off the main thread — decoding + JPEG compression can
        // take a noticeable moment and would otherwise freeze the UI.
        Uri photoUri = capturedPhotoUri;
        long expiresAt = selectedExpiresAtMillis;
        double lat = pickupLat;
        double lng = pickupLng;
        new Thread(() -> {
            try {
                String photoBase64 = ImageUtils.compressImageToBase64(this, photoUri, 800, 55);

                runOnUiThread(() -> Toast.makeText(PostFoodActivity.this,
                        "Verifying photo...", Toast.LENGTH_SHORT).show());

                AiImageChecker.checkImage(photoBase64, new AiImageChecker.CheckCallback() {
                    @Override
                    public void onResult(AiImageChecker.Result result) {
                        runOnUiThread(() -> {
                            if (result.isSuspicious()) {
                                setLoading(false);
                                showSuspiciousPhotoDialog(result, photoBase64, donorName, foodName,
                                        quantity, description, expiresAt, pickupLocation, pickupWindow, lat, lng);
                            } else {
                                uploadPost(photoBase64, donorName, foodName, quantity, description,
                                        expiresAt, pickupLocation, pickupWindow, lat, lng);
                            }
                        });
                    }

                    @Override
                    public void onCheckFailed(String message) {
                        // Fail open — a network/API hiccup shouldn't block a
                        // real donor from posting. Still show the error so
                        // it's visible during testing/debugging instead of
                        // silently disappearing.
                        runOnUiThread(() -> {
                            Toast.makeText(PostFoodActivity.this,
                                    "AI check failed (posting anyway): " + message, Toast.LENGTH_LONG).show();
                            uploadPost(photoBase64, donorName, foodName, quantity,
                                    description, expiresAt, pickupLocation, pickupWindow, lat, lng);
                        });
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(PostFoodActivity.this,
                            "Could not process photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * The AI check flagged something (not real food / looks AI-generated /
     * looks like a screenshot). This is a heuristic, not proof, so the
     * donor can either retake the photo or post anyway.
     */
    private void showSuspiciousPhotoDialog(AiImageChecker.Result result, String photoBase64,
                                            String donorName, String foodName, String quantity,
                                            String description, long expiresAt, String pickupLocation,
                                            String pickupWindow, double lat, double lng) {
        StringBuilder message = new StringBuilder();
        if (result.isScreenshot) {
            message.append("This looks like a screenshot rather than a live camera photo.\n");
        }
        if (result.isAiGenerated) {
            message.append("This image looks AI-generated rather than a real photo.\n");
        }
        if (!result.isRealFood) {
            message.append("This doesn't look like a photo of real food.\n");
        }
        if (!TextUtils.isEmpty(result.reason)) {
            message.append("\n").append(result.reason);
        }
        message.append("\n\nOnly genuine, live photos of real food are allowed.");

        new AlertDialog.Builder(this)
                .setTitle("Photo flagged")
                .setMessage(message.toString())
                .setCancelable(false)
                .setNegativeButton("Retake Photo", (dialog, which) -> resetToCameraView())
                .setPositiveButton("Post Anyway", (dialog, which) -> confirmPolicyOverride(result, photoBase64,
                        donorName, foodName, quantity, description, expiresAt, pickupLocation, pickupWindow, lat, lng))
                .show();
    }

    /**
     * Second, explicit confirmation before overriding an AI warning — makes
     * clear that doing so gets reported to admin, rather than silently
     * letting a tap on "Post Anyway" bypass the check.
     */
    private void confirmPolicyOverride(AiImageChecker.Result result, String photoBase64,
                                        String donorName, String foodName, String quantity,
                                        String description, long expiresAt, String pickupLocation,
                                        String pickupWindow, double lat, double lng) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Policy Override")
                .setMessage("Posting this photo despite the AI's warning will be reported to the admin "
                        + "for review, and may affect your account status. Do you want to continue?")
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> { /* stay on the form, do nothing */ })
                .setPositiveButton("Yes, Post Anyway", (dialog, which) -> {
                    foodPostRepository.reportPolicyViolation(donorName, foodName, result.reason,
                            result.isRealFood, result.isAiGenerated, result.isScreenshot);
                    uploadPost(photoBase64, donorName, foodName, quantity, description,
                            expiresAt, pickupLocation, pickupWindow, lat, lng);
                })
                .show();
    }

    private void uploadPost(String photoBase64, String donorName, String foodName, String quantity,
                             String description, long expiresAt, String pickupLocation,
                             String pickupWindow, double lat, double lng) {
        setLoading(true);
        foodPostRepository.createFoodPost(photoBase64, donorName, foodName,
                quantity, description, expiresAt, pickupLocation, pickupWindow, lat, lng,
                new FoodPostRepository.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(PostFoodActivity.this, "Food posted successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(PostFoodActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.btnSubmit.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}