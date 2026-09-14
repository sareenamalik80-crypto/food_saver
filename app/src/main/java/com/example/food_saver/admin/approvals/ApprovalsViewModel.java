package com.example.food_saver.admin.approvals;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Uses the real "users" collection schema: role in {donor, ngo},
 * status in {pending, verified, rejected} (see AuthRepository/User).
 */
public class ApprovalsViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");

    private final MutableLiveData<ApprovalsUiState> uiState =
            new MutableLiveData<>(ApprovalsUiState.initial());

    // Full, unfiltered result for the current status tab. The search box
    // filters this list locally instead of re-querying Firestore on every keystroke.
    private List<PendingAccount> rawAccounts = new ArrayList<>();

    public ApprovalsViewModel() {
        loadAccounts();
    }

    public LiveData<ApprovalsUiState> getUiState() {
        return uiState;
    }

    public void loadAccounts() {
        String status = currentOrInitial().getStatusFilter();
        uiState.setValue(currentOrInitial().withLoading(true));

        usersRef.whereIn("role", Arrays.asList("donor", "ngo"))
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<PendingAccount> accounts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        PendingAccount account = doc.toObject(PendingAccount.class);
                        account.setUid(doc.getId());
                        accounts.add(account);
                    }
                    rawAccounts = accounts;
                    applySearch(currentOrInitial().getSearchQuery());
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Failed to load accounts")));
    }

    public void setStatusFilter(String status) {
        if (status.equals(currentOrInitial().getStatusFilter())) return;
        uiState.setValue(currentOrInitial().withStatusFilter(status));
        loadAccounts();
    }

    public void setSearchQuery(String query) {
        applySearch(query);
    }

    private void applySearch(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<PendingAccount> filtered = new ArrayList<>();
        for (PendingAccount account : rawAccounts) {
            if (needle.isEmpty() || matches(account, needle)) {
                filtered.add(account);
            }
        }
        uiState.setValue(currentOrInitial().withSearchQuery(query == null ? "" : query, filtered));
    }

    private boolean matches(PendingAccount account, String needle) {
        String name = account.getName() != null ? account.getName().toLowerCase(Locale.getDefault()) : "";
        String email = account.getEmail() != null ? account.getEmail().toLowerCase(Locale.getDefault()) : "";
        return name.contains(needle) || email.contains(needle);
    }

    public void approve(PendingAccount account) {
        updateStatus(account, "verified", "Approved " + safeName(account));
    }

    public void reject(PendingAccount account) {
        updateStatus(account, "rejected", "Rejected " + safeName(account));
    }

    public void revokeToPending(PendingAccount account) {
        updateStatus(account, "pending", "Moved " + safeName(account) + " back to pending");
    }

    private void updateStatus(PendingAccount account, String newStatus, String successMessage) {
        usersRef.document(account.getUid())
                .update("status", newStatus)
                .addOnSuccessListener(unused -> {
                    rawAccounts = new ArrayList<>(rawAccounts);
                    rawAccounts.remove(account);
                    List<PendingAccount> filtered = new ArrayList<>(currentOrInitial().getAccounts());
                    filtered.remove(account);
                    uiState.setValue(currentOrInitial().withAccounts(filtered).withInfo(successMessage));
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Action failed")));
    }

    private String safeName(PendingAccount account) {
        return account.getName() != null ? account.getName() : "account";
    }

    private ApprovalsUiState currentOrInitial() {
        ApprovalsUiState current = uiState.getValue();
        return current != null ? current : ApprovalsUiState.initial();
    }
}
