package com.example.food_saver.admin.approvals;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityApprovalsBinding;
import com.google.android.material.snackbar.Snackbar;

public class ApprovalsActivity extends AppCompatActivity {

    private ActivityApprovalsBinding binding;
    private ApprovalsViewModel viewModel;
    private ApprovalsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApprovalsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ApprovalsViewModel.class);

        setupList();
        setupToolbar();
        setupSearch();
        setupStatusTabs();
        observeUiState();
    }

    private void setupList() {
        adapter = new ApprovalsAdapter(new ApprovalsAdapter.Listener() {
            @Override
            public void onApprove(PendingAccount account) {
                viewModel.approve(account);
            }

            @Override
            public void onReject(PendingAccount account) {
                viewModel.reject(account);
            }

            @Override
            public void onRevokeToPending(PendingAccount account) {
                viewModel.revokeToPending(account);
            }

            @Override
            public void onItemClick(PendingAccount account) {
                Intent intent = new Intent(ApprovalsActivity.this, AccountDetailActivity.class);
                intent.putExtra(AccountDetailActivity.EXTRA_UID, account.getUid());
                startActivity(intent);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> viewModel.loadAccounts());
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

    private void setupStatusTabs() {
        binding.statusToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == binding.btnFilterPending.getId()) {
                viewModel.setStatusFilter("pending");
            } else if (checkedId == binding.btnFilterApproved.getId()) {
                viewModel.setStatusFilter("verified");
            } else if (checkedId == binding.btnFilterRejected.getId()) {
                viewModel.setStatusFilter("rejected");
            }
        });
    }

    private void observeUiState() {
        viewModel.getUiState().observe(this, state -> {
            binding.progressBar.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
            adapter.submitList(state.getAccounts());

            boolean isEmpty = !state.isLoading() && state.getAccounts().isEmpty();
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
    protected void onResume() {
        super.onResume();
        // Approve/Reject can also happen from AccountDetailActivity, which
        // writes to Firestore directly rather than through this ViewModel —
        // refresh here so returning to this list always reflects that.
        viewModel.loadAccounts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
