package com.example.food_saver.admin.dashboard;

public class AdminDashboardUiState {

    private final boolean isLoading;
    private final long totalDonors;
    private final long totalNgos;
    private final long pendingApprovals;
    private final long totalDonations;
    private final long activeDonations;
    private final String errorMessage;

    public AdminDashboardUiState(boolean isLoading, long totalDonors, long totalNgos,
                                 long pendingApprovals, long totalDonations,
                                 long activeDonations, String errorMessage) {
        this.isLoading = isLoading;
        this.totalDonors = totalDonors;
        this.totalNgos = totalNgos;
        this.pendingApprovals = pendingApprovals;
        this.totalDonations = totalDonations;
        this.activeDonations = activeDonations;
        this.errorMessage = errorMessage;
    }

    public static AdminDashboardUiState initial() {
        return new AdminDashboardUiState(true, 0, 0, 0, 0, 0, null);
    }

    public boolean isLoading() { return isLoading; }
    public long getTotalDonors() { return totalDonors; }
    public long getTotalNgos() { return totalNgos; }
    public long getPendingApprovals() { return pendingApprovals; }
    public long getTotalDonations() { return totalDonations; }
    public long getActiveDonations() { return activeDonations; }
    public String getErrorMessage() { return errorMessage; }

    public AdminDashboardUiState withLoading(boolean loading) {
        return new AdminDashboardUiState(loading, totalDonors, totalNgos, pendingApprovals, totalDonations, activeDonations, errorMessage);
    }

    public AdminDashboardUiState withTotalDonors(long value) {
        return new AdminDashboardUiState(isLoading, value, totalNgos, pendingApprovals, totalDonations, activeDonations, errorMessage);
    }

    public AdminDashboardUiState withTotalNgos(long value) {
        return new AdminDashboardUiState(isLoading, totalDonors, value, pendingApprovals, totalDonations, activeDonations, errorMessage);
    }

    public AdminDashboardUiState withPendingApprovals(long value) {
        return new AdminDashboardUiState(isLoading, totalDonors, totalNgos, value, totalDonations, activeDonations, errorMessage);
    }

    public AdminDashboardUiState withTotalDonations(long value) {
        return new AdminDashboardUiState(isLoading, totalDonors, totalNgos, pendingApprovals, value, activeDonations, errorMessage);
    }

    public AdminDashboardUiState withActiveDonations(long value, boolean loading) {
        return new AdminDashboardUiState(loading, totalDonors, totalNgos, pendingApprovals, totalDonations, value, errorMessage);
    }

    public AdminDashboardUiState withError(String message) {
        return new AdminDashboardUiState(false, totalDonors, totalNgos, pendingApprovals, totalDonations, activeDonations, message);
    }
}
