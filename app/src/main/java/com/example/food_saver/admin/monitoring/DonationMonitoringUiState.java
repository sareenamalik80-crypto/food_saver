package com.example.food_saver.admin.monitoring;

import java.util.Collections;
import java.util.List;

public class DonationMonitoringUiState {

    private final boolean isLoading;
    private final List<DonationItem> donations;
    private final String searchQuery;
    private final String errorMessage;
    private final String infoMessage;

    public DonationMonitoringUiState(boolean isLoading, List<DonationItem> donations, String searchQuery,
                                      String errorMessage, String infoMessage) {
        this.isLoading = isLoading;
        this.donations = donations;
        this.searchQuery = searchQuery;
        this.errorMessage = errorMessage;
        this.infoMessage = infoMessage;
    }

    public static DonationMonitoringUiState initial() {
        return new DonationMonitoringUiState(true, Collections.emptyList(), "", null, null);
    }

    public boolean isLoading() { return isLoading; }
    public List<DonationItem> getDonations() { return donations; }
    public String getSearchQuery() { return searchQuery; }
    public String getErrorMessage() { return errorMessage; }
    public String getInfoMessage() { return infoMessage; }

    public DonationMonitoringUiState withLoading(boolean loading) {
        return new DonationMonitoringUiState(loading, donations, searchQuery, errorMessage, infoMessage);
    }

    public DonationMonitoringUiState withDonations(List<DonationItem> newDonations) {
        return new DonationMonitoringUiState(false, newDonations, searchQuery, null, null);
    }

    public DonationMonitoringUiState withSearchQuery(String newQuery, List<DonationItem> filtered) {
        return new DonationMonitoringUiState(false, filtered, newQuery, null, null);
    }

    public DonationMonitoringUiState withError(String message) {
        return new DonationMonitoringUiState(false, donations, searchQuery, message, null);
    }

    public DonationMonitoringUiState withInfo(String message) {
        return new DonationMonitoringUiState(false, donations, searchQuery, null, message);
    }
}
