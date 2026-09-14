package com.example.food_saver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityLoginBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.User;
import com.example.food_saver.ngo.NgoDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.root);

        authRepository = new AuthRepository();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        binding.tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = binding.etPassword.getSelectionEnd();
        binding.etPassword.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etPassword.setSelection(selection);
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
     * whether an account is even allowed in yet. This is the NGO app, so
     * only ROLE_NGO actually has somewhere real to go — Admin/Donor
     * accounts get a clear message instead of a broken navigation.
     */
    private void routeUser(User user) {
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            Toast.makeText(this, "This account is an Admin account — please use the Admin app.", Toast.LENGTH_LONG).show();
            authRepository.logout();
            return;
        }

        if (User.ROLE_DONOR.equals(user.getRole())) {
            Toast.makeText(this, "This account is a Donor account — please use the Donor app.", Toast.LENGTH_LONG).show();
            authRepository.logout();
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

        // status == verified, role == ngo
        startActivity(new Intent(this, NgoDashboardActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
