package com.example.food_saver.admin.monitoring;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DonationMonitoringViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference donationsRef = db.collection("foodPosts");

    private final MutableLiveData<DonationMonitoringUiState> uiState =
            new MutableLiveData<>(DonationMonitoringUiState.initial());

    // Full, unfiltered result from Firestore. The search box filters this
    // list locally instead of re-querying on every keystroke.
    private List<DonationItem> rawDonations = new ArrayList<>();

    public DonationMonitoringViewModel() {
        loadDonations();
    }

    public LiveData<DonationMonitoringUiState> getUiState() {
        return uiState;
    }

    public void loadDonations() {
        uiState.setValue(currentOrInitial().withLoading(true));

        Query query = donationsRef.orderBy("postedAt", Query.Direction.DESCENDING);
        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<DonationItem> donations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        DonationItem item = doc.toObject(DonationItem.class);
                        item.setId(doc.getId());
                        donations.add(item);
                    }
                    rawDonations = donations;
                    applySearch(currentOrInitial().getSearchQuery());
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Failed to load donations")));
    }

    public void setSearchQuery(String query) {
        applySearch(query);
    }

    private void applySearch(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<DonationItem> filtered = new ArrayList<>();
        for (DonationItem item : rawDonations) {
            if (needle.isEmpty() || matches(item, needle)) {
                filtered.add(item);
            }
        }
        uiState.setValue(currentOrInitial().withSearchQuery(query == null ? "" : query, filtered));
    }

    private boolean matches(DonationItem item, String needle) {
        String title = item.getFoodName() != null ? item.getFoodName().toLowerCase(Locale.getDefault()) : "";
        String donor = item.getDonorName() != null ? item.getDonorName().toLowerCase(Locale.getDefault()) : "";
        return title.contains(needle) || donor.contains(needle);
    }

    public void removeDonation(DonationItem item) {
        donationsRef.document(item.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    rawDonations = new ArrayList<>(rawDonations);
                    rawDonations.remove(item);
                    List<DonationItem> filtered = new ArrayList<>(currentOrInitial().getDonations());
                    filtered.remove(item);
                    uiState.setValue(currentOrInitial().withDonations(filtered).withInfo("Donation removed"));
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Failed to remove donation")));
    }

    private DonationMonitoringUiState currentOrInitial() {
        DonationMonitoringUiState current = uiState.getValue();
        return current != null ? current : DonationMonitoringUiState.initial();
    }
}
