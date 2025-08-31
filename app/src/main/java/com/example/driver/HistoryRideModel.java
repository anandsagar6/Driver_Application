package com.example.driver;

public class HistoryRideModel {
    private String rideId;
    private String pickupName;
    private String Drop;
    private String bookingDate;
    private String bookingTime;
    private String price;
    private String status;
    private String startTime;
    private String endTime;
    private String driverAcceptTime;   // ✅ missing
    private String customerId;         // ✅ missing
    private String cancelledTime;      // ✅ missing

    public HistoryRideModel() { }

    // Getters & Setters
    public String getDrop() { return Drop; }
    public void setDrop(String drop) { Drop = drop; }


    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDriverAcceptTime() { return driverAcceptTime; }
    public void setDriverAcceptTime(String driverAcceptTime) { this.driverAcceptTime = driverAcceptTime; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCancelledTime() { return cancelledTime; }
    public void setCancelledTime(String cancelledTime) { this.cancelledTime = cancelledTime; }
}
