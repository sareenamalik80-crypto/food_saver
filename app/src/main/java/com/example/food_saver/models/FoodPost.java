package com.example.food_saver.models;

public class FoodPost {

    private String foodId;
    private String donorId;
    private String donorName;
    private String foodName;
    private String imageUrl;
    private String quantity;
    private String description;
    private long postedAt;
    private long expiresAt;
    private String status;

    private String claimedByNgoId;
    private String claimedByNgoName;

    private String pickupLocation;
    private String pickupWindow;

    // GPS coordinates captured from the donor's device at post time — lets
    // this NGO sort/display distance to the pickup point.
    private double pickupLat;
    private double pickupLng;

    public FoodPost() {}

    public FoodPost(String foodId, String donorId, String donorName, String foodName,
                    String imageUrl, String quantity, String description,
                    long postedAt, long expiresAt) {
        this.foodId = foodId;
        this.donorId = donorId;
        this.donorName = donorName;
        this.foodName = foodName;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.description = description;
        this.postedAt = postedAt;
        this.expiresAt = expiresAt;
        this.status = "available";
    }

    public String getFoodId() { return foodId; }
    public void setFoodId(String foodId) { this.foodId = foodId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getPostedAt() { return postedAt; }
    public void setPostedAt(long postedAt) { this.postedAt = postedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClaimedByNgoId() { return claimedByNgoId; }
    public void setClaimedByNgoId(String claimedByNgoId) { this.claimedByNgoId = claimedByNgoId; }

    public String getClaimedByNgoName() { return claimedByNgoName; }
    public void setClaimedByNgoName(String claimedByNgoName) { this.claimedByNgoName = claimedByNgoName; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getPickupWindow() { return pickupWindow; }
    public void setPickupWindow(String pickupWindow) { this.pickupWindow = pickupWindow; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }
}