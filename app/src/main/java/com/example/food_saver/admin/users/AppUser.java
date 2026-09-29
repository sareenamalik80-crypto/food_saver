package com.example.food_saver.admin.users;


public class AppUser {

    private String uid;
    private String name;
    private String email;
    private String role;
    private String accountStatus; // "active" or "suspended"
    private boolean flagged;
    private long flagCount;

    public AppUser() {
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public long getFlagCount() { return flagCount; }
    public void setFlagCount(long flagCount) { this.flagCount = flagCount; }

    public boolean isSuspended() {
        return "suspended".equalsIgnoreCase(accountStatus);
    }
}
