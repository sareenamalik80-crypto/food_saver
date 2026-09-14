package com.example.food_saver.ngo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityRateDonorBinding;
import com.example.food_saver.models.Rating;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;

public class RateDonorActivity extends AppCompatActivity {

    public static final String EXTRA_DONOR_ID = "extra_donor_id";

    private ActivityRateDonorBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private String foodId;
    private String donorId;
    private String myNgoId;
    private String myNgoName = "My NGO"; // apni NGO profile se replace karein

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRateDonorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        foodId = getIntent().getStringExtra(NgoDashboardActivity.EXTRA_FOOD_ID);
        donorId = getIntent().getStringExtra(EXTRA_DONOR_ID);
        String donorName = getIntent().getStringExtra(FoodDetailNgoActivity.EXTRA_DONOR_NAME);
        myNgoId = FirebaseAuth.getInstance().getUid();

        binding.tvDonorName.setText(donorName);

        binding.btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void submitRating() {
        float stars = binding.ratingBar.getRating();
        String comment = binding.etComment.getText().toString().trim();

        if (stars == 0f) {
            Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
            return;
        }

        Rating rating = new Rating(foodId, donorId, myNgoId, myNgoName, stars, comment, System.currentTimeMillis());

        repository.submitRating(rating, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(RateDonorActivity.this, "Thank you! Rating submitted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RateDonorActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}