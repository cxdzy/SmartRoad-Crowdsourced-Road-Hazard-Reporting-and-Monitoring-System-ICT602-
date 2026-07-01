package com.example.smartroad;

public class HazardReport {
    public String id;
    public String uid;
    public String type;
    public String description;
    public double latitude;
    public double longitude;
    public String photoUrl;
    public String status;
    public String timestamp;
    public String userAgent;

    // Required by Firebase for deserialization
    public HazardReport() {}
}
