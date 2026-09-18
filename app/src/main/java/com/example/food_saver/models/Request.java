package com.example.food_saver.models;

import com.google.firebase.firestore.Exclude;

/**
 * Represents an NGO's request on a donor's food post.
 * Firestore collection "requests/{requestId}" — field names match exactly
 * what the NGO module writes (agreed with teammate):
 * requestId, foodPostId, ngoId, ngoName, donorId, status, requestedAt, respondedAt.
 *
 * There is deliberately NO foodPostTitle / foodPostImageUrl here — the
 * Donor's "NGO Requests" screen fetches the linked foodPosts/{foodPostId}
 * document itself to get foodName + imageUrl (see RequestRepository,
 * which populates the two transient fields below after that fetch).
 *
 * Status flow (matches FoodPost's flow): requested -> approved (donor
 * accepts) or back to available on the FoodPost (donor rejects) ->
 * collected -> handedOver.
 */
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

    // Dual-confirmation handover flags. Both sides must independently mark
    // their half before the request (and the linked FoodPost) is moved to
    // STATUS_HANDED_OVER. Firestore defaults a missing boolean field to
    // false, so existing/older documents are safely treated as unconfirmed.
    // NGO module: please write to "ngoConfirmedReceived" using this exact
    // field name when your teammate implements the "Mark as Received" button.
    private boolean donorConfirmedHandover;
    private boolean ngoConfirmedReceived;

    // NOT stored in Firestore on this collection — populated locally after
    // fetching the linked foodPosts/{foodPostId} document, purely so the
    // adapter has something to display without a second round-trip per bind.
    // @Exclude tells Firestore's mapper to ignore these when reading/writing.
    private String foodName;
    private String foodImageBase64;

    // Empty constructor required for Firestore deserialization
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