package com.example.food_saver.ngo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.auth.LoginActivity;
import com.example.food_saver.databinding.ActivityNgoProfileBinding;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private ActivityNgoProfileBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private String myNgoId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNgoProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        myNgoId = FirebaseAuth.getInstance().getUid();

        String email = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "Unknown";
        binding.tvEmail.setText(email);

        loadProfile();

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navTransparency, binding.navProfile, BottomNavHelper.Tab.PROFILE);
    }

    private void loadProfile() {
        if (myNgoId == null) return;
        repository.getNgoProfile(myNgoId, new FoodRepository.ProfileCallback() {
            @Override
            public void onProfile(String name, String phone, String address) {
                binding.etNgoName.setText(name != null ? name : "");
                binding.etPhone.setText(phone != null ? phone : "");
                binding.etAddress.setText(address != null ? address : "");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Couldn't load profile: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        if (myNgoId == null) return;

        String name = binding.etNgoName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your organization name.", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.saveNgoProfile(myNgoId, name, phone, address, new FoodRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, "Couldn't save profile: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
