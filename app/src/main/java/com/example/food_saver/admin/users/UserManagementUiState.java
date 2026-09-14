package com.example.food_saver.admin.users;

import java.util.Collections;
import java.util.List;

public class UserManagementUiState {

    private final boolean isLoading;
    private final List<AppUser> users;
    private final String roleFilter; // "all" | "donor" | "ngo"
    private final String searchQuery;
    private final String errorMessage;
    private final String infoMessage;

    public UserManagementUiState(boolean isLoading, List<AppUser> users, String roleFilter,
                                  String searchQuery, String errorMessage, String infoMessage) {
        this.isLoading = isLoading;
        this.users = users;
        this.roleFilter = roleFilter;
        this.searchQuery = searchQuery;
        this.errorMessage = errorMessage;
        this.infoMessage = infoMessage;
    }

    public static UserManagementUiState initial() {
        return new UserManagementUiState(true, Collections.emptyList(), "all", "", null, null);
    }

    public boolean isLoading() { return isLoading; }
    public List<AppUser> getUsers() { return users; }
    public String getRoleFilter() { return roleFilter; }
    public String getSearchQuery() { return searchQuery; }
    public String getErrorMessage() { return errorMessage; }
    public String getInfoMessage() { return infoMessage; }

    public UserManagementUiState withLoading(boolean loading) {
        return new UserManagementUiState(loading, users, roleFilter, searchQuery, errorMessage, infoMessage);
    }

    public UserManagementUiState withUsers(List<AppUser> newUsers) {
        return new UserManagementUiState(false, newUsers, roleFilter, searchQuery, null, null);
    }

    public UserManagementUiState withRoleFilter(String newRoleFilter) {
        return new UserManagementUiState(true, users, newRoleFilter, searchQuery, null, null);
    }

    public UserManagementUiState withSearchQuery(String newQuery, List<AppUser> filtered) {
        return new UserManagementUiState(false, filtered, roleFilter, newQuery, null, null);
    }

    public UserManagementUiState withError(String message) {
        return new UserManagementUiState(false, users, roleFilter, searchQuery, message, null);
    }

    public UserManagementUiState withInfo(String message) {
        return new UserManagementUiState(false, users, roleFilter, searchQuery, null, message);
    }
}
