package com.example.food_saver.admin.monitoring;

import com.google.firebase.firestore.PropertyName;


public class DonationItem {

    private String id;
    private String foodName;
    private String donorName;
    private String quantity;
    private String status;
    private Long postedAt;
    private Long expiresAt;

    public DonationItem() {

    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    @PropertyName("donorName")
    public String getDonorName() { return donorName; }
    @PropertyName("donorName")
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getPostedAt() { return postedAt; }
    public void setPostedAt(Long postedAt) { this.postedAt = postedAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    @com.google.firebase.firestore.Exclude
    public String getEffectiveStatus() {
        boolean open = "available".equals(status) || "requested".equals(status) || "approved".equals(status);
        if (open && expiresAt != null && expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            return "expired";
        }
        return status;
    }
}
