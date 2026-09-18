package com.example.food_saver.donor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.auth.AuthRepository;
import com.example.food_saver.auth.LoginActivity;
import com.example.food_saver.databinding.ActivityProfileBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.User;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private AuthRepository authRepository;
    private final FoodPostRepository foodPostRepository = new FoodPostRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        authRepository = new AuthRepository();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navRequests, binding.navProfile, BottomNavHelper.Tab.PROFILE);

        loadProfile();
        loadRating();
    }

    private void loadRating() {
        foodPostRepository.getMyRatingStats(new FoodPostRepository.RatingStatsCallback() {
            @Override
            public void onStats(float average, int count) {
                if (count == 0) {
                    binding.tvRating.setText("No ratings from NGOs yet");
                } else {
                    binding.tvRating.setText(String.format(Locale.getDefault(),
                            "\u2b50 %.1f (%d rating%s from NGOs)", average, count, count == 1 ? "" : "s"));
                }
            }

            @Override
            public void onError(String message) {
                binding.tvRating.setText("");
            }
        });
    }

    private void loadProfile() {
        if (authRepository.getCurrentUser() == null) return;

        authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        binding.tvName.setText(user.getName());
                        binding.tvEmail.setText(user.getEmail());
                        binding.tvPhone.setText(user.getPhone());
                        binding.tvAddress.setText(user.getAddress());
                        binding.tvStatus.setText("Status: " + capitalize(user.getStatus()));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
