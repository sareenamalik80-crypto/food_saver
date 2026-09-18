package com.example.food_saver.models;

/**
 * Represents a user document stored in Firestore under "users/{uid}".
 * role: "admin" | "donor" | "ngo"
 * status: "pending" | "verified" | "rejected"  (admin accounts are always "verified")
 */
public class User {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_DONOR = "donor";
    public static final String ROLE_NGO = "ngo";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_REJECTED = "rejected";

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String status;
    private String orgDocUrl;   // NGO registration doc / CNIC image, optional for donor
    private long createdAt;

    // Empty constructor required for Firestore deserialization
    public User() {
    }

    public User(String uid, String name, String email, String phone,
                String address, String role, String status,
                String orgDocUrl, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.status = status;
        this.orgDocUrl = orgDocUrl;
        this.createdAt = createdAt;
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

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}