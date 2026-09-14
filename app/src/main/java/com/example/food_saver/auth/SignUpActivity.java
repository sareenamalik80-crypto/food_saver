package com.example.food_saver.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivitySignupBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.User;
import com.example.food_saver.utils.ImageUtils;

import java.io.IOException;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private AuthRepository authRepository;
    private Uri selectedDocumentUri;
    private boolean passwordVisible = false;

    private final ActivityResultLauncher<String> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedDocumentUri = uri;
                    binding.ivDocumentPreview.setVisibility(View.VISIBLE);
                    binding.ivDocumentPreview.setImageURI(uri);
                    binding.tvDocumentStatus.setText("Document selected ✓");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.root);

        authRepository = new AuthRepository();

        binding.btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        updateDocumentLabelForRole();
        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> updateDocumentLabelForRole());

        binding.btnUploadDocument.setOnClickListener(v -> documentPickerLauncher.launch("image/*"));

        binding.btnSignUp.setOnClickListener(v -> attemptSignUp());

        binding.tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
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

    /** Swaps the hint text so the requirement matches the selected role. */
    private void updateDocumentLabelForRole() {
        boolean isDonor = binding.rbDonor.isChecked();
        binding.tvDocumentLabel.setText(isDonor
                ? "Upload Food Authority Letter"
                : "Upload NGO License Certificate");
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

        if (selectedDocumentUri == null) {
            Toast.makeText(this, "Please upload your verification document", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = binding.rbDonor.isChecked() ? User.ROLE_DONOR : User.ROLE_NGO;

        setLoading(true);

        // Compress off the main thread — decoding + JPEG compression can
        // take a noticeable moment and would otherwise freeze the UI.
        Uri documentUri = selectedDocumentUri;
        new Thread(() -> {
            try {
                String documentBase64 = ImageUtils.compressImageToBase64(this, documentUri, 1024, 60);

                runOnUiThread(() -> authRepository.signUp(name, email, password, phone, address,
                        role, documentBase64, new AuthRepository.AuthCallback() {
                            @Override
                            public void onSuccess(User user) {
                                setLoading(false);
                                Toast.makeText(SignUpActivity.this,
                                        "Account created. Please wait for admin verification.",
                                        Toast.LENGTH_LONG).show();
                                startActivity(new Intent(SignUpActivity.this, PendingVerificationActivity.class));
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                setLoading(false);
                                Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }));

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(SignUpActivity.this,
                            "Could not process document image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        binding.btnSignUp.setEnabled(!loading);
        binding.btnUploadDocument.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}