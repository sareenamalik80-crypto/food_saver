package com.example.food_saver.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityAdminProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminProfileActivity extends AppCompatActivity {

    private ActivityAdminProfileBinding binding;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        String email = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "Unknown";
        binding.tvEmail.setText(email);

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        loadProfile();

        binding.btnSave.setOnClickListener(v -> saveProfile());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AdminLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfile() {
        if (uid == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(this::populateFields)
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile load nahi ho saka: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFields(DocumentSnapshot doc) {
        binding.progressBar.setVisibility(View.GONE);
        if (!doc.exists()) return;

        String name = doc.getString("name");
        String phone = doc.getString("phone");
        String address = doc.getString("address");

        binding.etName.setText(name != null ? name : "");
        binding.etPhone.setText(phone != null ? phone : "");
        binding.etAddress.setText(address != null ? address : "");
    }

    private void saveProfile() {
        if (uid == null) return;

        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
        String address = binding.etAddress.getText() != null ? binding.etAddress.getText().toString().trim() : "";

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(unused -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Profile update ho gaya", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, "Save nahi ho saka: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}