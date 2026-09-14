package com.example.food_saver.admin.dashboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityAdminLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class AdminLoginActivity extends AppCompatActivity {

    private ActivityAdminLoginBinding binding;
    private FirebaseAuth auth;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Email");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        String currentEmail = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        input.setText(currentEmail);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, 0);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your admin email — we'll send a link to reset your password.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Enter your email first.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> Toast.makeText(AdminLoginActivity.this,
                                    "Password reset link sent — check your email.", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(AdminLoginActivity.this,
                                    e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .show();
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = binding.etPassword.getSelectionEnd();
        binding.etPassword.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etPassword.setSelection(selection);
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            e.getMessage() != null ? e.getMessage() : "Login failed. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }
}
