package com.example.food_saver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityLoginBinding;
import com.example.food_saver.models.User;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-route if a session is already active (skip login screen).
        if (authRepository.getCurrentUser() != null) {
            setLoading(true);
            authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(User user) {
                            setLoading(false);
                            routeUser(user);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            setLoading(false);
                            // Profile missing/corrupt — force a clean re-login.
                            authRepository.logout();
                        }
                    });
        }
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                routeUser(user);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Central routing point: role decides which dashboard, status decides
     * whether a donor/NGO account is even allowed in yet.
     * NOTE: replace the placeholder class names below with your real
     * dashboard Activities as each module gets built.
     */
    private void routeUser(User user) {
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            // startActivity(new Intent(this, AdminDashboardActivity.class));
            Toast.makeText(this, "Route to Admin Dashboard", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (User.STATUS_PENDING.equals(user.getStatus())) {
            startActivity(new Intent(this, PendingVerificationActivity.class));
            finish();
            return;
        }

        if (User.STATUS_REJECTED.equals(user.getStatus())) {
            Toast.makeText(this, "Your account was not approved. Contact support.", Toast.LENGTH_LONG).show();
            authRepository.logout();
            return;
        }

        // status == verified
        if (User.ROLE_DONOR.equals(user.getRole())) {
            // startActivity(new Intent(this, DonorHomeActivity.class));
            Toast.makeText(this, "Route to Donor Home", Toast.LENGTH_SHORT).show();
        } else if (User.ROLE_NGO.equals(user.getRole())) {
            // startActivity(new Intent(this, NgoHomeActivity.class));
            Toast.makeText(this, "Route to NGO Home", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}