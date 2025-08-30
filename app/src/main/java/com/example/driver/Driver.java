package com.example.driver;

public class Driver {
    public String driverId;
    public String firstName;
    public String lastName;
    public String phone;
    public String vehicle;
    public String dlNumber;
    public String email;
    public String status;

    // Empty constructor required for Firebase
    public Driver() {
    }

    // ✅ Constructor
    public Driver(String firstName, String lastName, String phone, String vehicle,
                  String dlNumber, String email, String status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.vehicle = vehicle;
        this.dlNumber = dlNumber;
        this.email = email;
        this.status = status;
    }
}
