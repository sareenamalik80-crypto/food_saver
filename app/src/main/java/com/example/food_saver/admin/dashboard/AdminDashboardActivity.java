package com.example.food_saver.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.food_saver.admin.approvals.ApprovalsActivity;
import com.example.food_saver.admin.monitoring.DonationMonitoringActivity;
import com.example.food_saver.admin.users.UserManagementActivity;
import com.example.food_saver.admin.violations.PolicyViolationsActivity;
import com.example.food_saver.databinding.ActivityAdminDashboardBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private AdminDashboardViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);

        setupStatLabels();
        setupHeader();
        setupClickListeners();
        observeUiState();
    }

    private void setupStatLabels() {
        binding.statDonors.tvLabel.setText("Total Donors");
        binding.statNgos.tvLabel.setText("Total NGOs");
        binding.statTotalDonations.tvLabel.setText("Total Donations");
        binding.statActiveDonations.tvLabel.setText("Active Donations");
    }

    private void setupHeader() {
        binding.tvWelcome.setText("Welcome, Admin");
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            binding.tvWelcome.setText("Welcome, " + name);
                        }
                    });
        }

        binding.btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfileActivity.class)));

        binding.btnRefresh.setOnClickListener(v -> viewModel.loadDashboardStats());
    }

    private void setupClickListeners() {
        binding.cardApprovals.setOnClickListener(v ->
                startActivity(new Intent(this, ApprovalsActivity.class)));

        binding.cardMonitoring.setOnClickListener(v ->
                startActivity(new Intent(this, DonationMonitoringActivity.class)));

        binding.cardUserManagement.setOnClickListener(v ->
                startActivity(new Intent(this, UserManagementActivity.class)));

        binding.cardFlagged.setOnClickListener(v ->
                startActivity(new Intent(this, PolicyViolationsActivity.class)));
    }

    private void observeUiState() {
        viewModel.getUiState().observe(this, state -> {
            binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);

            binding.statDonors.tvValue.setText(String.valueOf(state.getTotalDonors()));
            binding.statNgos.tvValue.setText(String.valueOf(state.getTotalNgos()));
            binding.statTotalDonations.tvValue.setText(String.valueOf(state.getTotalDonations()));
            binding.statActiveDonations.tvValue.setText(String.valueOf(state.getActiveDonations()));

            binding.tvPendingCount.setText(String.valueOf(state.getPendingApprovals()));
            binding.tvFlaggedCount.setText(String.valueOf(state.getPendingViolations()));

            if (state.getErrorMessage() != null) {
                Snackbar.make(binding.getRoot(), state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}