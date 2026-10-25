package com.example.food_saver.ngo;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityDeliveryDetailsBinding;
import com.example.food_saver.models.DeliveryDetails;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.utils.TimeUtils;
import com.example.food_saver.utils.ValidationUtils;

public class DeliveryDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";

    private ActivityDeliveryDetailsBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private String requestId;
    private int arrivalMinute = -1;
    private int[] pickupWindow;
    private String pickupWindowText;
    private boolean postExpired;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeliveryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        String foodId = getIntent().getStringExtra(NgoDashboardActivity.EXTRA_FOOD_ID);
        if (requestId == null) {
            Toast.makeText(this, "Something went wrong — missing request.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPickupWindow(foodId);

        binding.etTimeOfArrival.setOnClickListener(v -> showTimePicker());
        binding.btnSaveDelivery.setOnClickListener(v -> saveDetails());
    }

    private void loadPickupWindow(String foodId) {
        if (foodId == null) {
            binding.tvPickupWindowInfo.setVisibility(View.GONE);
            return;
        }
        repository.getPost(foodId, new FoodRepository.SinglePostCallback() {
            @Override
            public void onPost(FoodPost post) {
                if (post.isExpired()) {
                    postExpired = true;
                    binding.tvPickupWindowInfo.setText("This donation has expired, so delivery details can't be added.");
                    binding.tvPickupWindowInfo.setTextColor(getColor(com.example.food_saver.R.color.error_red));
                    binding.btnSaveDelivery.setEnabled(false);
                    binding.btnSaveDelivery.setAlpha(0.5f);
                    return;
                }
                pickupWindowText = post.getPickupWindow();
                pickupWindow = TimeUtils.parseWindow(pickupWindowText);
                if (pickupWindow != null) {
                    pickupWindowText = TimeUtils.formatWindow(pickupWindow[0], pickupWindow[1]);
                }
                binding.tvPickupWindowInfo.setText("Donor's pickup time: "
                        + (pickupWindowText != null ? pickupWindowText : "not specified")
                        + "\nThe rider must arrive within this time.");
                if (arrivalMinute >= 0) showArrivalError(arrivalError());
            }

            @Override
            public void onError(Exception e) {
                binding.tvPickupWindowInfo.setText("Couldn't load the donor's pickup time.");
            }
        });
    }

    private void showTimePicker() {
        int initial = arrivalMinute >= 0 ? arrivalMinute
                : (pickupWindow != null ? Math.max(pickupWindow[0], TimeUtils.nowMinuteOfDay())
                : TimeUtils.nowMinuteOfDay());
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            arrivalMinute = hourOfDay * 60 + minute;
            binding.etTimeOfArrival.setText(TimeUtils.format(arrivalMinute));
            String error = arrivalError();
            showArrivalError(error);
            if (error != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Invalid arrival time")
                        .setMessage(error)
                        .setPositiveButton("Choose Again", (d, w) -> showTimePicker())
                        .setNegativeButton("OK", null)
                        .show();
            }
        }, initial / 60, initial % 60, false);
        dialog.setTitle("Rider arrival time");
        dialog.show();
    }

    private String arrivalError() {
        if (arrivalMinute < 0) return "Please select the rider's arrival time";
        if (pickupWindow != null && !TimeUtils.isInWindow(arrivalMinute, pickupWindow)) {
            return "Kindly select the rider's arrival time within the donor's pickup time ("
                    + pickupWindowText + ").";
        }
        if (arrivalMinute < TimeUtils.nowMinuteOfDay()) {
            return "The rider's arrival time can't be in the past. Please select a later time.";
        }
        return null;
    }

    private void showArrivalError(String error) {
        if (error == null) {
            binding.tvArrivalError.setVisibility(View.GONE);
            ValidationUtils.clear(binding.etTimeOfArrival);
        } else {
            binding.tvArrivalError.setText(error);
            binding.tvArrivalError.setVisibility(View.VISIBLE);
        }
    }

    private void saveDetails() {
        if (postExpired) {
            Toast.makeText(this, "This donation has expired.", Toast.LENGTH_SHORT).show();
            return;
        }
        String riderName = binding.etRiderName.getText().toString().trim();
        String riderPhone = binding.etRiderPhone.getText().toString().trim();
        String vehicleNumber = binding.etVehicleNumber.getText().toString().trim().toUpperCase();
        String arrivalErrorText = arrivalError();

        ValidationUtils.Form form = new ValidationUtils.Form()
                .check(binding.etRiderName, ValidationUtils.personName("Rider name", riderName))
                .check(binding.etRiderPhone, ValidationUtils.phone(riderPhone))
                .check(binding.etVehicleNumber, ValidationUtils.vehicleNumber(vehicleNumber));
        showArrivalError(arrivalErrorText);
        if (!form.isValid() || arrivalErrorText != null) {
            Toast.makeText(this, "Please correct the highlighted fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        DeliveryDetails details = new DeliveryDetails(riderName, riderPhone.replaceAll("[\\s-]", ""),
                vehicleNumber, TimeUtils.format(arrivalMinute), System.currentTimeMillis());
        details.setArrivalAt(TimeUtils.todayAt(arrivalMinute));

        binding.btnSaveDelivery.setEnabled(false);
        repository.saveDeliveryDetails(requestId, details, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(DeliveryDetailsActivity.this, "Delivery details saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                binding.btnSaveDelivery.setEnabled(true);
                Toast.makeText(DeliveryDetailsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
