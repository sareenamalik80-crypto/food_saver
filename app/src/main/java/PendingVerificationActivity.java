package com.example.food_saver.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityPendingVerificationBinding;
import com.example.food_saver.models.User;

/**
 * Shown after signup (and on login) while a donor/NGO account's
 * status is still "pending". A "Refresh status" button re-checks
 * Firestore in case admin has verified them in the meantime.
 */
public class PendingVerificationActivity extends AppCompatActivity {

    private ActivityPendingVerificationBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.btnRefresh.setOnClickListener(v -> checkStatus());
        binding.btnLogout.setOnClickListener(v -> {
            authRepository.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void checkStatus() {
        if (authRepository.getCurrentUser() == null) return;

        authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (User.STATUS_VERIFIED.equals(user.getStatus())) {
                            startActivity(new Intent(PendingVerificationActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            binding.tvStatusMessage.setText(
                                    User.STATUS_REJECTED.equals(user.getStatus())
                                            ? "Your account was not approved. Contact support."
                                            : "Still under review. Please check back soon.");
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        binding.tvStatusMessage.setText(errorMessage);
                    }
                });
    }
}