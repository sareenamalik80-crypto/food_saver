package com.example.food_saver.models;

public class FoodRequest {

    private String requestId;
    private String foodPostId;
    private String ngoId;
    private String ngoName;
    private String donorId;
    private String status;      // pending / approved / rejected
    private long requestedAt;
    private long respondedAt;

    public FoodRequest() {}

    public FoodRequest(String requestId, String foodPostId, String ngoId, String ngoName,
                       String donorId, long requestedAt) {
        this.requestId = requestId;
        this.foodPostId = foodPostId;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
        this.donorId = donorId;
        this.status = "requested";
        this.requestedAt = requestedAt;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getFoodPostId() { return foodPostId; }
    public void setFoodPostId(String foodPostId) { this.foodPostId = foodPostId; }

    public String getNgoId() { return ngoId; }
    public void setNgoId(String ngoId) { this.ngoId = ngoId; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    public long getRespondedAt() { return respondedAt; }
    public void setRespondedAt(long respondedAt) { this.respondedAt = respondedAt; }

    // Dual-confirmation handover flags. Both sides must independently mark
    // their half before the request (and the linked FoodPost) moves to
    // "handedOver". donorConfirmedHandover is written by the Donor module —
    // this NGO module only reads it. ngoConfirmedReceived is written here.
    private boolean donorConfirmedHandover;
    private boolean ngoConfirmedReceived;

    public boolean isDonorConfirmedHandover() { return donorConfirmedHandover; }
    public void setDonorConfirmedHandover(boolean donorConfirmedHandover) { this.donorConfirmedHandover = donorConfirmedHandover; }

    public boolean isNgoConfirmedReceived() { return ngoConfirmedReceived; }
    public void setNgoConfirmedReceived(boolean ngoConfirmedReceived) { this.ngoConfirmedReceived = ngoConfirmedReceived; }
}