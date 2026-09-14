package com.example.food_saver.admin.approvals;

import java.util.Collections;
import java.util.List;

public class ApprovalsUiState {

    private final boolean isLoading;
    private final List<PendingAccount> accounts;
    private final String statusFilter; // "pending" | "approved" | "rejected"
    private final String searchQuery;
    private final String errorMessage;
    private final String infoMessage;

    public ApprovalsUiState(boolean isLoading, List<PendingAccount> accounts, String statusFilter,
                             String searchQuery, String errorMessage, String infoMessage) {
        this.isLoading = isLoading;
        this.accounts = accounts;
        this.statusFilter = statusFilter;
        this.searchQuery = searchQuery;
        this.errorMessage = errorMessage;
        this.infoMessage = infoMessage;
    }

    public static ApprovalsUiState initial() {
        return new ApprovalsUiState(true, Collections.emptyList(), "pending", "", null, null);
    }

    public boolean isLoading() { return isLoading; }
    public List<PendingAccount> getAccounts() { return accounts; }
    public String getStatusFilter() { return statusFilter; }
    public String getSearchQuery() { return searchQuery; }
    public String getErrorMessage() { return errorMessage; }
    public String getInfoMessage() { return infoMessage; }

    public ApprovalsUiState withLoading(boolean loading) {
        return new ApprovalsUiState(loading, accounts, statusFilter, searchQuery, errorMessage, infoMessage);
    }

    public ApprovalsUiState withAccounts(List<PendingAccount> newAccounts) {
        return new ApprovalsUiState(false, newAccounts, statusFilter, searchQuery, null, null);
    }

    public ApprovalsUiState withStatusFilter(String newStatusFilter) {
        return new ApprovalsUiState(true, accounts, newStatusFilter, searchQuery, null, null);
    }

    public ApprovalsUiState withSearchQuery(String newQuery, List<PendingAccount> filtered) {
        return new ApprovalsUiState(false, filtered, statusFilter, newQuery, null, null);
    }

    public ApprovalsUiState withError(String message) {
        return new ApprovalsUiState(false, accounts, statusFilter, searchQuery, message, null);
    }

    public ApprovalsUiState withInfo(String message) {
        return new ApprovalsUiState(false, accounts, statusFilter, searchQuery, null, message);
    }
}
