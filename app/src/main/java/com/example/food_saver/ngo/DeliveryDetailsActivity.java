package com.example.food_saver.ngo;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityDeliveryDetailsBinding;
import com.example.food_saver.models.DeliveryDetails;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;

import java.util.Calendar;

public class DeliveryDetailsActivity extends AppCompatActivity {

    private ActivityDeliveryDetailsBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private String foodId;
    private String selectedTime = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeliveryDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        foodId = getIntent().getStringExtra(NgoDashboardActivity.EXTRA_FOOD_ID);

        binding.etTimeOfArrival.setOnClickListener(v -> showTimePicker());

        binding.btnSaveDelivery.setOnClickListener(v -> saveDetails());
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            selectedTime = android.text.format.DateFormat.format("hh:mm a", c).toString();
            binding.etTimeOfArrival.setText(selectedTime);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
        dialog.show();
    }

    private void saveDetails() {
        String riderName = binding.etRiderName.getText().toString().trim();
        String riderPhone = binding.etRiderPhone.getText().toString().trim();
        String vehicleNumber = binding.etVehicleNumber.getText().toString().trim();

        if (riderName.isEmpty() || riderPhone.isEmpty() || vehicleNumber.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please fill in all the details", Toast.LENGTH_SHORT).show();
            return;
        }

        DeliveryDetails details = new DeliveryDetails(riderName, riderPhone, vehicleNumber,
                selectedTime, System.currentTimeMillis());

        repository.saveDeliveryDetails(foodId, details, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(DeliveryDetailsActivity.this, "Delivery details saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(DeliveryDetailsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}