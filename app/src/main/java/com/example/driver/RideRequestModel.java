package com.example.driver;

public class RideRequestModel {
    private String requestId;
    private String customerName;
    private String pickup;
    private String destination;
    private String price;
    private String rideType;
    private String status;
    private String pickupName;
    private String Drop;

    public RideRequestModel() {
        // Needed for Firebase
    }

    public RideRequestModel(String customerName, String pickup, String destination, String price,
                            String rideType, String status, String pickupName, String drop) {
        this.customerName = customerName;
        this.pickup = pickup;
        this.destination = destination;
        this.price = price;
        this.rideType = rideType;
        this.status = status;
        this.pickupName = pickupName;
        this.Drop = drop;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getRideType() { return rideType; }
    public void setRideType(String rideType) { this.rideType = rideType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getDrop() { return Drop; }
    public void setDrop(String drop) { this.Drop = drop; }
}
