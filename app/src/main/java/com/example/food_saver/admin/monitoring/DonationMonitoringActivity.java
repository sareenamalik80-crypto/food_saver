package com.example.food_saver.admin.monitoring;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityDonationMonitoringBinding;
import com.google.android.material.snackbar.Snackbar;

public class DonationMonitoringActivity extends AppCompatActivity {

    private ActivityDonationMonitoringBinding binding;
    private DonationMonitoringViewModel viewModel;
    private DonationMonitoringAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDonationMonitoringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DonationMonitoringViewModel.class);

        setupList();
        setupToolbar();
        setupSearch();
        observeUiState();
    }

    private void setupList() {
        adapter = new DonationMonitoringAdapter(item -> confirmRemove(item));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void confirmRemove(DonationItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Remove donation")
                .setMessage("Remove \"" + (item.getFoodName() != null ? item.getFoodName() : "this donation")
                        + "\"? This can't be undone.")
                .setPositiveButton("Remove", (dialog, which) -> viewModel.removeDonation(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> viewModel.loadDonations());
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeUiState() {
        viewModel.getUiState().observe(this, state -> {
            binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getDonations());

            boolean isEmpty = !state.isLoading() && state.getDonations().isEmpty();
            binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            if (state.getErrorMessage() != null) {
                Snackbar.make(binding.getRoot(), state.getErrorMessage(), Snackbar.LENGTH_LONG).show();
            } else if (state.getInfoMessage() != null) {
                Snackbar.make(binding.getRoot(), state.getInfoMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
