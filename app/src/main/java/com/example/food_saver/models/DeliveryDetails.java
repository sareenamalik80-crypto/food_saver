package com.example.food_saver.models;

public class DeliveryDetails {

    private String riderName;
    private String riderPhone;
    private String vehicleNumber;
    private String timeOfArrival;
    private long filledAt;

    public DeliveryDetails() {}

    public DeliveryDetails(String riderName, String riderPhone, String vehicleNumber,
                           String timeOfArrival, long filledAt) {
        this.riderName = riderName;
        this.riderPhone = riderPhone;
        this.vehicleNumber = vehicleNumber;
        this.timeOfArrival = timeOfArrival;
        this.filledAt = filledAt;
    }

    public String getRiderName() { return riderName; }
    public void setRiderName(String riderName) { this.riderName = riderName; }

    public String getRiderPhone() { return riderPhone; }
    public void setRiderPhone(String riderPhone) { this.riderPhone = riderPhone; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getTimeOfArrival() { return timeOfArrival; }
    public void setTimeOfArrival(String timeOfArrival) { this.timeOfArrival = timeOfArrival; }

    public long getFilledAt() { return filledAt; }
    public void setFilledAt(long filledAt) { this.filledAt = filledAt; }
}