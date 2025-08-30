package com.example.driver;

public class HistoryRideModel {
    private String Drop;             // exact match
    private String driverAcceptTime;
    private String driverId;
    private String driverName;
    private String vehicle;      // match Firebase
    private String bookingDate;
    private String bookingTime;
    private String customerId;
    private double destLat;
    private double destLng;
    private double pickupLat;
    private double pickupLng;
    private String pickupName;
    private String pin;
    private String price;
    private String rideId;
    private String rideType;
    private String status;
    private String startTime;
    private String endTime;

    public HistoryRideModel() { }

    // Getters & Setters
    public String getDrop() { return Drop; }
    public void setDrop(String drop) { Drop = drop; }

    public String getDriverAcceptTime() { return driverAcceptTime; }
    public void setDriverAcceptTime(String driverAcceptTime) { this.driverAcceptTime = driverAcceptTime; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getVehicle() { return vehicle; }
    public void setVehicle(String vehicle) { this.vehicle = vehicle; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getRideType() { return rideType; }
    public void setRideType(String rideType) { this.rideType = rideType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
