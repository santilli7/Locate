package com.example.locate;

public class Location {

    public String address;
    public double latitude, longitude;

    public Location(String mAddress, double mLatitude, double mLongitude) {
        this.address = mAddress;
        this.latitude = mLatitude;
        this.longitude = mLongitude;
    }

    public Location() {

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
