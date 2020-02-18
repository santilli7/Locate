package com.example.locate.ui.emergency;

import android.os.Parcel;
import android.os.Parcelable;

public class Emergency extends Object implements Parcelable {
    private double latitude, longitude;
    private String priority, address;

    public Emergency(double latitude, double longitude, String priority) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.priority = priority;
    }

    public Emergency() {

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    protected Emergency(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        priority = in.readString();
        address = in.readString();
    }

    public static final Creator<Emergency> CREATOR = new Creator<Emergency>() {
        @Override
        public Emergency createFromParcel(Parcel in) {
            return new Emergency(in);
        }

        @Override
        public Emergency[] newArray(int size) {
            return new Emergency[size];
        }
    };

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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(priority);
        dest.writeString(address);
    }
}
