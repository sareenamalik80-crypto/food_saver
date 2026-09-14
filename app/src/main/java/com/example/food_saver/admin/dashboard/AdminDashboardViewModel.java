package com.example.food_saver.admin.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class AdminDashboardViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");
    private final CollectionReference donationsRef = db.collection("foodPosts");

    private final MutableLiveData<AdminDashboardUiState> uiState =
            new MutableLiveData<>(AdminDashboardUiState.initial());

    public AdminDashboardViewModel() {
        loadDashboardStats();
    }

    public LiveData<AdminDashboardUiState> getUiState() {
        return uiState;
    }

    public void loadDashboardStats() {
        AdminDashboardUiState current = uiState.getValue();
        uiState.setValue(current != null ? current.withLoading(true) : AdminDashboardUiState.initial());

        countQuery(usersRef.whereEqualTo("role", "donor"), count ->
                uiState.setValue(uiState.getValue().withTotalDonors(count)));

        countQuery(usersRef.whereEqualTo("role", "ngo"), count ->
                uiState.setValue(uiState.getValue().withTotalNgos(count)));

        countQuery(
                usersRef.whereIn("role", Arrays.asList("donor", "ngo"))
                        .whereEqualTo("status", "pending"),
                count -> uiState.setValue(uiState.getValue().withPendingApprovals(count))
        );

        countQuery(donationsRef, count ->
                uiState.setValue(uiState.getValue().withTotalDonations(count)));

        countQuery(donationsRef.whereEqualTo("status", "available"), count ->
                uiState.setValue(uiState.getValue().withActiveDonations(count, false)));

        countQuery(db.collection("policyViolations").whereEqualTo("reviewed", false), count ->
                uiState.setValue(uiState.getValue().withPendingViolations(count)));
    }

    private interface CountCallback {
        void onCount(long count);
    }

    private void countQuery(@NonNull Query query, @NonNull CountCallback callback) {
        query.count().get(AggregateSource.SERVER)
                .addOnSuccessListener(snapshot -> callback.onCount(snapshot.getCount()))
                .addOnFailureListener(e -> {
                    AdminDashboardUiState current = uiState.getValue();
                    uiState.setValue(
                            (current != null ? current : AdminDashboardUiState.initial())
                                    .withError(e.getMessage() != null ? e.getMessage() : "Failed to load stats")
                    );
                });
    }
}