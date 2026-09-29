package com.example.food_saver.admin.approvals;

public class PendingAccount {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String status;
    private String orgDocUrl; // Base64 verification document (License Certificate / Food Authority Letter)

    public PendingAccount() {

    }

    public PendingAccount(String uid, String name, String email, String role, String status) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrgDocUrl() { return orgDocUrl; }
    public void setOrgDocUrl(String orgDocUrl) { this.orgDocUrl = orgDocUrl; }
}
