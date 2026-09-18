package com.example.food_saver.models;

public class Rating {

    private String ratingId;
    private String foodPostId;
    private String donorId;
    private String ngoId;
    private String ngoName;
    private float stars;
    private String comment;
    private long timestamp;

    public Rating() {}

    public Rating(String foodPostId, String donorId, String ngoId, String ngoName,
                  float stars, String comment, long timestamp) {
        this.foodPostId = foodPostId;
        this.donorId = donorId;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
        this.stars = stars;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }

    public String getFoodPostId() { return foodPostId; }
    public void setFoodPostId(String foodPostId) { this.foodPostId = foodPostId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getNgoId() { return ngoId; }
    public void setNgoId(String ngoId) { this.ngoId = ngoId; }

    public String getNgoName() { return ngoName; }
    public void setNgoName(String ngoName) { this.ngoName = ngoName; }

    public float getStars() { return stars; }
    public void setStars(float stars) { this.stars = stars; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}