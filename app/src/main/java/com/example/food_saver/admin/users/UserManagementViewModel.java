package com.example.food_saver.admin.users;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class UserManagementViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");

    private final MutableLiveData<UserManagementUiState> uiState =
            new MutableLiveData<>(UserManagementUiState.initial());

    // Full, unfiltered result for the current role tab. The search box
    // filters this list locally instead of re-querying Firestore on every keystroke.
    private List<AppUser> rawUsers = new ArrayList<>();

    public UserManagementViewModel() {
        loadUsers();
    }

    public LiveData<UserManagementUiState> getUiState() {
        return uiState;
    }

    public void loadUsers() {
        String roleFilter = currentOrInitial().getRoleFilter();
        uiState.setValue(currentOrInitial().withLoading(true));

        Query query = "all".equals(roleFilter)
                ? usersRef.whereIn("role", Arrays.asList("donor", "ngo"))
                : usersRef.whereEqualTo("role", roleFilter);

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<AppUser> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        AppUser user = doc.toObject(AppUser.class);
                        user.setUid(doc.getId());
                        users.add(user);
                    }
                    rawUsers = users;
                    applySearch(currentOrInitial().getSearchQuery());
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Failed to load users")));
    }

    public void setRoleFilter(String roleFilter) {
        if (roleFilter.equals(currentOrInitial().getRoleFilter())) return;
        uiState.setValue(currentOrInitial().withRoleFilter(roleFilter));
        loadUsers();
    }

    public void setSearchQuery(String query) {
        applySearch(query);
    }

    private void applySearch(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<AppUser> filtered = new ArrayList<>();
        for (AppUser user : rawUsers) {
            if (needle.isEmpty() || matches(user, needle)) {
                filtered.add(user);
            }
        }
        uiState.setValue(currentOrInitial().withSearchQuery(query == null ? "" : query, filtered));
    }

    private boolean matches(AppUser user, String needle) {
        String name = user.getName() != null ? user.getName().toLowerCase(Locale.getDefault()) : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase(Locale.getDefault()) : "";
        return name.contains(needle) || email.contains(needle);
    }

    public void toggleSuspend(AppUser user) {
        String newStatus = user.isSuspended() ? "active" : "suspended";
        usersRef.document(user.getUid())
                .update("accountStatus", newStatus)
                .addOnSuccessListener(unused -> {
                    updateLocalStatus(user, newStatus);
                    String message = "suspended".equals(newStatus) ? "User suspended" : "User reactivated";
                    uiState.setValue(currentOrInitial().withInfo(message));
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Action failed")));
    }

    private void updateLocalStatus(AppUser user, String newStatus) {
        for (AppUser candidate : rawUsers) {
            if (candidate.getUid().equals(user.getUid())) {
                candidate.setAccountStatus(newStatus);
                break;
            }
        }
        for (AppUser candidate : currentOrInitial().getUsers()) {
            if (candidate.getUid().equals(user.getUid())) {
                candidate.setAccountStatus(newStatus);
                break;
            }
        }
    }

    public void deleteUser(AppUser user) {
        usersRef.document(user.getUid())
                .delete()
                .addOnSuccessListener(unused -> {
                    rawUsers = new ArrayList<>(rawUsers);
                    rawUsers.remove(user);
                    List<AppUser> filtered = new ArrayList<>(currentOrInitial().getUsers());
                    filtered.remove(user);
                    uiState.setValue(currentOrInitial().withUsers(filtered).withInfo("User removed"));
                })
                .addOnFailureListener(e -> uiState.setValue(currentOrInitial()
                        .withError(e.getMessage() != null ? e.getMessage() : "Failed to remove user")));
    }

    private UserManagementUiState currentOrInitial() {
        UserManagementUiState current = uiState.getValue();
        return current != null ? current : UserManagementUiState.initial();
    }
}
