package com.example.food_saver.admin.users;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityUserManagementBinding;
import com.google.android.material.snackbar.Snackbar;

public class UserManagementActivity extends AppCompatActivity {

    private ActivityUserManagementBinding binding;
    private UserManagementViewModel viewModel;
    private UserManagementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(UserManagementViewModel.class);

        setupList();
        setupToolbar();
        setupSearch();
        setupRoleTabs();
        observeUiState();
    }

    private void setupList() {
        adapter = new UserManagementAdapter(new UserManagementAdapter.Listener() {
            @Override
            public void onToggleSuspend(AppUser user) {
                viewModel.toggleSuspend(user);
            }

            @Override
            public void onDelete(AppUser user) {
                confirmDelete(user);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void confirmDelete(AppUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Remove user")
                .setMessage("Remove " + (user.getName() != null ? user.getName() : "this user")
                        + "? This can't be undone.")
                .setPositiveButton("Remove", (dialog, which) -> viewModel.deleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> viewModel.loadUsers());
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

    private void setupRoleTabs() {
        binding.roleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == binding.btnFilterAll.getId()) {
                viewModel.setRoleFilter("all");
            } else if (checkedId == binding.btnFilterDonors.getId()) {
                viewModel.setRoleFilter("donor");
            } else if (checkedId == binding.btnFilterNgos.getId()) {
                viewModel.setRoleFilter("ngo");
            }
        });
    }

    private void observeUiState() {
        viewModel.getUiState().observe(this, state -> {
            binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getUsers());

            boolean isEmpty = !state.isLoading() && state.getUsers().isEmpty();
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
