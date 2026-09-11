package com.example.food_saver.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivitySignupBinding;
import com.example.food_saver.models.User;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authRepository = new AuthRepository();

        binding.btnSignUp.setOnClickListener(v -> attemptSignUp());

        binding.tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptSignUp() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = binding.rbDonor.isChecked() ? User.ROLE_DONOR : User.ROLE_NGO;

        setLoading(true);

        authRepository.signUp(name, email, password, phone, address, role, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(SignUpActivity.this,
                        "Account created. Please wait for admin verification.",
                        Toast.LENGTH_LONG).show();
                // Pending accounts always land on a "waiting for verification" screen,
                // regardless of role, until admin approves them.
                startActivity(new Intent(SignUpActivity.this, PendingVerificationActivity.class));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.btnSignUp.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}