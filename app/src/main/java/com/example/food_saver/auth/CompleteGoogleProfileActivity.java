package com.example.food_saver.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityCompleteGoogleProfileBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.User;
import com.example.food_saver.utils.ImageUtils;

import java.io.IOException;

public class CompleteGoogleProfileActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_EMAIL = "extra_email";

    private ActivityCompleteGoogleProfileBinding binding;
    private AuthRepository authRepository;
    private Uri selectedDocumentUri;
    private String uid;
    private String name;
    private String email;

    private final ActivityResultLauncher<String> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedDocumentUri = uri;
                    binding.ivDocumentPreview.setVisibility(View.VISIBLE);
                    binding.ivDocumentPreview.setImageURI(uri);
                    binding.tvDocumentStatus.setText("Document selected \u2713");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompleteGoogleProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.root);

        authRepository = new AuthRepository();

        uid = getIntent().getStringExtra(EXTRA_UID);
        name = getIntent().getStringExtra(EXTRA_NAME);
        email = getIntent().getStringExtra(EXTRA_EMAIL);

        if (TextUtils.isEmpty(uid)) {
            // Shouldn't happen — bail out to Login rather than crash later.
            Toast.makeText(this, "Something went wrong. Please try signing in again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.tvGoogleName.setText(!TextUtils.isEmpty(name) ? name : "(no name on Google account)");
        binding.tvGoogleEmail.setText(email != null ? email : "");

        updateDocumentLabelForRole();
        binding.rgRole.setOnCheckedChangeListener((group, checkedId) -> updateDocumentLabelForRole());

        binding.btnUploadDocument.setOnClickListener(v -> documentPickerLauncher.launch("image/*"));

        binding.btnComplete.setOnClickListener(v -> attemptComplete());
    }

    private void updateDocumentLabelForRole() {
        boolean isDonor = binding.rbDonor.isChecked();
        binding.tvDocumentLabel.setText(isDonor
                ? "Upload Food Authority Letter"
                : "Upload NGO License Certificate");
    }

    private void attemptComplete() {
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDocumentUri == null) {
            Toast.makeText(this, "Please upload your verification document", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = binding.rbDonor.isChecked() ? User.ROLE_DONOR : User.ROLE_NGO;

        setLoading(true);

        Uri documentUri = selectedDocumentUri;
        new Thread(() -> {
            try {
                String documentBase64 = ImageUtils.compressImageToBase64(this, documentUri, 1024, 60);

                runOnUiThread(() -> authRepository.completeGoogleSignUp(uid, name, email, phone, address,
                        role, documentBase64, new AuthRepository.AuthCallback() {
                            @Override
                            public void onSuccess(User user) {
                                setLoading(false);
                                Toast.makeText(CompleteGoogleProfileActivity.this,
                                        "Account created. Please wait for admin verification.",
                                        Toast.LENGTH_LONG).show();
                                startActivity(new Intent(CompleteGoogleProfileActivity.this,
                                        PendingVerificationActivity.class));
                                finish();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                setLoading(false);
                                Toast.makeText(CompleteGoogleProfileActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }));

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(CompleteGoogleProfileActivity.this,
                            "Could not process document image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        binding.btnComplete.setEnabled(!loading);
        binding.btnUploadDocument.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
