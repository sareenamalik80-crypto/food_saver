package com.example.food_saver.admin.violations;

/**
 * A record of a donor overriding the AI photo check (posting/saving a photo
 * the AI flagged as not-real-food / AI-generated / a screenshot).
 * Firestore collection "policyViolations/{violationId}".
 */
public class PolicyViolation {

    private String violationId;
    private String donorId;
    private String donorName;
    private String foodName;
    private String aiReason;
    private boolean isRealFood;
    private boolean isAiGenerated;
    private boolean isScreenshot;
    private long timestamp;
    private boolean reviewed;

    public PolicyViolation() {
        // Required empty constructor for Firestore deserialization.
    }

    public String getViolationId() { return violationId; }
    public void setViolationId(String violationId) { this.violationId = violationId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getAiReason() { return aiReason; }
    public void setAiReason(String aiReason) { this.aiReason = aiReason; }

    public boolean isRealFood() { return isRealFood; }
    public void setRealFood(boolean realFood) { isRealFood = realFood; }

    public boolean isAiGenerated() { return isAiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }

    public boolean isScreenshot() { return isScreenshot; }
    public void setScreenshot(boolean screenshot) { isScreenshot = screenshot; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
}
