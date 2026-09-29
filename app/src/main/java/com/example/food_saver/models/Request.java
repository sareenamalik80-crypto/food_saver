package com.example.food_saver.models;

import com.google.firebase.firestore.Exclude;
public class Request {

    public static final String STATUS_REQUESTED = "requested";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_COLLECTED = "collected";
    public static final String STATUS_HANDED_OVER = "handedOver";

    private String requestId;
    private String foodPostId;
    private String ngoId;
    private String ngoName;
    private String donorId;
    private String status;
    private long requestedAt;
    private long respondedAt;

    // Delivery details — filled in by the NGO after the donor approves.
    private String riderName;
    private String vehicleNumber;
    private String riderPhone;
    private String arrivalTime;
    private boolean donorConfirmedHandover;
    private boolean ngoConfirmedReceived;
    private String foodName;
    private String foodImageBase64;
    public Request() {
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

    public boolean isDonorConfirmedHandover() { return donorConfirmedHandover; }
    public void setDonorConfirmedHandover(boolean donorConfirmedHandover) { this.donorConfirmedHandover = donorConfirmedHandover; }

    public boolean isNgoConfirmedReceived() { return ngoConfirmedReceived; }
    public void setNgoConfirmedReceived(boolean ngoConfirmedReceived) { this.ngoConfirmedReceived = ngoConfirmedReceived; }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getRiderPhone() { return riderPhone; }
    public void setRiderPhone(String riderPhone) { this.riderPhone = riderPhone; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    @Exclude
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    @Exclude
    public String getFoodImageBase64() { return foodImageBase64; }
    public void setFoodImageBase64(String foodImageBase64) { this.foodImageBase64 = foodImageBase64; }

    /** True once the NGO has filled in all four delivery fields. */
    @Exclude
    public boolean hasDeliveryDetails() {
        return riderName != null && !riderName.isEmpty()
                && vehicleNumber != null && !vehicleNumber.isEmpty()
                && riderPhone != null && !riderPhone.isEmpty()
                && arrivalTime != null && !arrivalTime.isEmpty();
    }
}