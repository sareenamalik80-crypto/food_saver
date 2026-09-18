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

import com.example.food_saver.databinding.ActivityEditFoodPostBinding;
import com.example.food_saver.utils.AiImageChecker;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.utils.ImageUtils;
import com.example.food_saver.utils.LocationHelper;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Lets a donor edit a food post they already created — only while it's
 * still "available" (nobody has requested it yet). Launched from
 * MyDonationsActivity with EXTRA_FOOD_ID; check FoodPost.isEditable()
 * before starting this Activity.
 */
public class EditFoodPostActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "extra_food_id";

    private ActivityEditFoodPostBinding binding;
    private FoodPostRepository foodPostRepository;

    private String foodId;
    private String donorName; // kept from the loaded post, only needed for policy-violation reporting
    private ImageCapture imageCapture;
    private Uri newPhotoUri; // null until the donor retakes the photo
    private String existingImageBase64;
    private long selectedExpiresAtMillis = -1L;
    private double pickupLat = 0.0;
    private double pickupLng = 0.0;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to retake the photo.", Toast.LENGTH_LONG).show();
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
        binding = ActivityEditFoodPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        foodId = getIntent().getStringExtra(EXTRA_FOOD_ID);
        if (TextUtils.isEmpty(foodId)) {
            Toast.makeText(this, "Missing post to edit.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        foodPostRepository = new FoodPostRepository();

        binding.etExpiryDate.setFocusable(false);
        binding.etExpiryDate.setOnClickListener(v -> showDatePicker());

        binding.btnUseLocation.setOnClickListener(v -> checkLocationPermissionAndFetch());
        binding.btnRetakePhoto.setOnClickListener(v -> switchToCameraView());
        binding.btnCapture.setOnClickListener(v -> capturePhoto());
        binding.btnCancelRetake.setOnClickListener(v -> switchToExistingPhotoView());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSaveChanges.setOnClickListener(v -> attemptSave());

        loadExistingPost();
    }

    private void loadExistingPost() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.formContainer.setVisibility(View.GONE);

        foodPostRepository.getFoodPost(foodId, new FoodPostRepository.SinglePostCallback() {
            @Override
            public void onLoaded(FoodPost post) {
                if (!post.isEditable()) {
                    Toast.makeText(EditFoodPostActivity.this,
                            "This post can no longer be edited — an NGO has already requested it.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                populateForm(post);
                binding.progressBar.setVisibility(View.GONE);
                binding.formContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditFoodPostActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void populateForm(FoodPost post) {
        donorName = post.getDonorName();
        existingImageBase64 = post.getImageUrl();
        byte[] imageBytes = ImageUtils.decodeBase64ToBytes(existingImageBase64);
        if (imageBytes.length > 0) {
            binding.ivExistingPhoto.setImageBitmap(
                    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
        }

        binding.etFoodTitle.setText(post.getFoodName());
        binding.etQuantity.setText(post.getQuantity());
        binding.etDescription.setText(post.getDescription());
        binding.etPickupLocation.setText(post.getPickupLocation());
        binding.etPickupWindow.setText(post.getPickupWindow());

        pickupLat = post.getPickupLat();
        pickupLng = post.getPickupLng();

        selectedExpiresAtMillis = post.getExpiresAt();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(post.getExpiresAt());
        binding.etExpiryDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)));
    }

    private void switchToCameraView() {
        binding.existingPhotoContainer.setVisibility(View.GONE);
        binding.cameraContainer.setVisibility(View.VISIBLE);
        checkCameraPermissionAndStart();
    }

    private void switchToExistingPhotoView() {
        binding.cameraContainer.setVisibility(View.GONE);
        binding.existingPhotoContainer.setVisibility(View.VISIBLE);
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
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
        // MediaStore gallery — sidesteps MediaStore write failures seen on
        // some OEM ROMs (e.g. "Failed to write to MediaStore URI: null" on
        // Huawei) since the photo is only ever used internally.
        java.io.File photoFile = new java.io.File(getCacheDir(),
                "foodbridge_edit_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        newPhotoUri = Uri.fromFile(photoFile);
                        binding.ivExistingPhoto.setImageURI(newPhotoUri);
                        switchToExistingPhotoView();
                        Toast.makeText(EditFoodPostActivity.this, "Photo updated — Save Changes to confirm.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(EditFoodPostActivity.this,
                                "Capture failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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
                                : String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude));
                binding.btnUseLocation.setEnabled(true);
                binding.btnUseLocation.setText("📍 Use My Current Location");
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditFoodPostActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                binding.btnUseLocation.setEnabled(true);
                binding.btnUseLocation.setText("📍 Use My Current Location");
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedExpiresAtMillis > 0) {
            calendar.setTimeInMillis(selectedExpiresAtMillis);
        }
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 23, 59, 59);
            selectedExpiresAtMillis = picked.getTimeInMillis();

            String formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            binding.etExpiryDate.setText(formatted);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void attemptSave() {
        String foodName = binding.etFoodTitle.getText().toString().trim();
        String quantity = binding.etQuantity.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String pickupLocation = binding.etPickupLocation.getText().toString().trim();
        String pickupWindow = binding.etPickupWindow.getText().toString().trim();

        if (TextUtils.isEmpty(foodName) || TextUtils.isEmpty(quantity) ||
                selectedExpiresAtMillis < 0 || TextUtils.isEmpty(pickupLocation) ||
                TextUtils.isEmpty(pickupWindow)) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        long expiresAt = selectedExpiresAtMillis;
        double lat = pickupLat;
        double lng = pickupLng;

        if (newPhotoUri == null) {
            // Photo wasn't retaken — update text fields only.
            foodPostRepository.updateFoodPost(foodId, null, foodName, quantity, description,
                    expiresAt, pickupLocation, pickupWindow, lat, lng, saveCallback());
        } else {
            // Compress the new photo off the main thread before saving.
            Uri photoUri = newPhotoUri;
            new Thread(() -> {
                try {
                    String photoBase64 = ImageUtils.compressImageToBase64(this, photoUri, 800, 55);

                    runOnUiThread(() -> Toast.makeText(EditFoodPostActivity.this,
                            "Verifying photo...", Toast.LENGTH_SHORT).show());

                    AiImageChecker.checkImage(photoBase64, new AiImageChecker.CheckCallback() {
                        @Override
                        public void onResult(AiImageChecker.Result result) {
                            runOnUiThread(() -> {
                                if (result.isSuspicious()) {
                                    setLoading(false);
                                    showSuspiciousPhotoDialog(result, photoBase64, foodName, quantity,
                                            description, expiresAt, pickupLocation, pickupWindow, lat, lng);
                                } else {
                                    foodPostRepository.updateFoodPost(foodId, photoBase64, foodName,
                                            quantity, description, expiresAt, pickupLocation, pickupWindow,
                                            lat, lng, saveCallback());
                                }
                            });
                        }

                        @Override
                        public void onCheckFailed(String message) {
                            // Fail open — a network/API hiccup shouldn't
                            // block a real donor from saving. Still show the
                            // error so it's visible during testing/debugging.
                            runOnUiThread(() -> {
                                Toast.makeText(EditFoodPostActivity.this,
                                        "AI check failed (saving anyway): " + message, Toast.LENGTH_LONG).show();
                                foodPostRepository.updateFoodPost(foodId, photoBase64,
                                        foodName, quantity, description, expiresAt, pickupLocation,
                                        pickupWindow, lat, lng, saveCallback());
                            });
                        }
                    });

                } catch (IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(EditFoodPostActivity.this,
                                "Could not process photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }
    }

    /**
     * The AI check flagged something (not real food / looks AI-generated /
     * looks like a screenshot). This is a heuristic, not proof, so the
     * donor can either retake the photo or save anyway.
     */
    private void showSuspiciousPhotoDialog(AiImageChecker.Result result, String photoBase64,
                                            String foodName, String quantity, String description,
                                            long expiresAt, String pickupLocation, String pickupWindow,
                                            double lat, double lng) {
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
                .setNegativeButton("Retake Photo", (dialog, which) -> switchToCameraView())
                .setPositiveButton("Save Anyway", (dialog, which) -> confirmPolicyOverride(result,
                        photoBase64, foodName, quantity, description, expiresAt, pickupLocation,
                        pickupWindow, lat, lng))
                .show();
    }

    /**
     * Second, explicit confirmation before overriding an AI warning — makes
     * clear that doing so gets reported to admin, rather than silently
     * letting a tap on "Save Anyway" bypass the check.
     */
    private void confirmPolicyOverride(AiImageChecker.Result result, String photoBase64,
                                        String foodName, String quantity, String description,
                                        long expiresAt, String pickupLocation, String pickupWindow,
                                        double lat, double lng) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Policy Override")
                .setMessage("Saving this photo despite the AI's warning will be reported to the admin "
                        + "for review, and may affect your account status. Do you want to continue?")
                .setCancelable(false)
                .setNegativeButton("Cancel", (dialog, which) -> { /* stay on the form, do nothing */ })
                .setPositiveButton("Yes, Save Anyway", (dialog, which) -> {
                    foodPostRepository.reportPolicyViolation(donorName, foodName, result.reason,
                            result.isRealFood, result.isAiGenerated, result.isScreenshot);
                    foodPostRepository.updateFoodPost(foodId, photoBase64, foodName, quantity,
                            description, expiresAt, pickupLocation, pickupWindow, lat, lng, saveCallback());
                })
                .show();
    }

    private FoodPostRepository.UploadCallback saveCallback() {
        return new FoodPostRepository.UploadCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(EditFoodPostActivity.this, "Changes saved!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(EditFoodPostActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void setLoading(boolean loading) {
        binding.btnSaveChanges.setEnabled(!loading);
        binding.savingProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}