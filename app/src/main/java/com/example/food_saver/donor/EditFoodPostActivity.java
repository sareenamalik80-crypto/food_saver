package com.example.food_saver.donor;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.food_saver.utils.TimeUtils;
import com.example.food_saver.utils.ValidationUtils;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class EditFoodPostActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "extra_food_id";

    private ActivityEditFoodPostBinding binding;
    private FoodPostRepository foodPostRepository;

    private String foodId;
    private ImageCapture imageCapture;
    private Uri newPhotoUri; // null until the donor retakes the photo
    private String existingImageBase64;
    private long selectedExpiresAtMillis = -1L;
    private double pickupLat = 0.0;
    private double pickupLng = 0.0;
    private int pickupStartMin = -1;
    private int pickupEndMin = -1;

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

        binding.etPickupWindow.setFocusable(false);
        binding.etPickupWindow.setOnClickListener(v -> showPickupWindowPicker());

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
        int[] window = TimeUtils.parseWindow(post.getPickupWindow());
        if (window != null) {
            pickupStartMin = window[0];
            pickupEndMin = window[1];
            binding.etPickupWindow.setText(TimeUtils.formatWindow(pickupStartMin, pickupEndMin));
        } else {
            binding.etPickupWindow.setText("");
        }

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
            ValidationUtils.clear(binding.etExpiryDate);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showPickupWindowPicker() {
        int initialStart = pickupStartMin >= 0 ? pickupStartMin : TimeUtils.nowMinuteOfDay();
        TimePickerDialog startDialog = new TimePickerDialog(this, (view, hour, minute) -> {
            int start = hour * 60 + minute;
            int initialEnd = pickupEndMin > start ? pickupEndMin : Math.min(start + 120, 23 * 60 + 59);
            TimePickerDialog endDialog = new TimePickerDialog(this, (endView, endHour, endMinute) -> {
                pickupStartMin = start;
                pickupEndMin = endHour * 60 + endMinute;
                binding.etPickupWindow.setText(TimeUtils.formatWindow(pickupStartMin, pickupEndMin));
                new ValidationUtils.Form().check(binding.etPickupWindow, pickupWindowError());
            }, initialEnd / 60, initialEnd % 60, false);
            endDialog.setTitle("Pickup ends at");
            endDialog.show();
        }, initialStart / 60, initialStart % 60, false);
        startDialog.setTitle("Pickup starts at");
        startDialog.show();
    }

    private String pickupWindowError() {
        if (pickupStartMin < 0 || pickupEndMin < 0) return "Please select the pickup time window";
        if (pickupEndMin <= pickupStartMin) return "Pickup end time must be after the start time";
        if (selectedExpiresAtMillis > 0 && android.text.format.DateUtils.isToday(selectedExpiresAtMillis)
                && pickupEndMin <= TimeUtils.nowMinuteOfDay()) {
            return "This pickup time has already passed today — please choose a later time";
        }
        return null;
    }

    private String expiryError() {
        if (selectedExpiresAtMillis < 0) return "Please select the expiry date";
        if (selectedExpiresAtMillis < System.currentTimeMillis()) return "This date has already passed — please choose a new expiry date";
        return null;
    }

    private void attemptSave() {
        String foodName = binding.etFoodTitle.getText().toString().trim();
        String quantity = binding.etQuantity.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String pickupLocation = binding.etPickupLocation.getText().toString().trim();
        String pickupWindow = binding.etPickupWindow.getText().toString().trim();

        ValidationUtils.Form form = new ValidationUtils.Form()
                .check(binding.etFoodTitle, ValidationUtils.foodName(foodName))
                .check(binding.etQuantity, ValidationUtils.quantity(quantity))
                .check(binding.etDescription, ValidationUtils.optionalText("Description", description,
                        ValidationUtils.DESCRIPTION_MAX))
                .check(binding.etExpiryDate, expiryError())
                .check(binding.etPickupLocation, ValidationUtils.address("Pickup location", pickupLocation))
                .check(binding.etPickupWindow, pickupWindowError());
        if (!form.isValid()) {
            Toast.makeText(this, "Please correct the highlighted fields.", Toast.LENGTH_SHORT).show();
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
                                    showPolicyViolationDialog(result);
                                } else {
                                    foodPostRepository.updateFoodPost(foodId, photoBase64, foodName,
                                            quantity, description, expiresAt, pickupLocation, pickupWindow,
                                            lat, lng, saveCallback());
                                }
                            });
                        }

                        @Override
                        public void onCheckFailed(String message) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                showCheckFailedDialog(message);
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

    private void showCheckFailedDialog(String message) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle("Couldn't verify photo")
                .setMessage(message + "\n\nYour photo must be verified before saving. "
                        + "Please check your internet connection and try again.")
                .setCancelable(false)
                .setPositiveButton("Try Again", (dialog, which) -> attemptSave())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPolicyViolationDialog(AiImageChecker.Result result) {
        if (isFinishing()) return;
        StringBuilder message = new StringBuilder();
        message.append("This photo violates our food posting policy, so the changes can't be saved.\n\n");
        if (result.isScreenshot) {
            message.append("• It looks like a screenshot, not a live camera photo.\n");
        }
        if (result.isAiGenerated) {
            message.append("• It looks AI-generated or edited, not a real photo.\n");
        }
        if (!result.isRealFood) {
            message.append("• It doesn't look like a photo of real food.\n");
        }
        if (!TextUtils.isEmpty(result.reason)) {
            message.append("\n").append(result.reason).append("\n");
        }
        message.append("\nPlease retake a clear, live photo of the actual food.");

        new AlertDialog.Builder(this)
                .setTitle("Policy Violation")
                .setMessage(message.toString())
                .setCancelable(false)
                .setPositiveButton("Retake Photo", (dialog, which) -> switchToCameraView())
                .setNegativeButton("Cancel", null)
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