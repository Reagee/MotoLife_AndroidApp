package com.app.motolife.model;

import java.sql.Timestamp;

import androidx.annotation.NonNull;

public class UserLocation {
    private int id;
    private String username;
    private String email;
    private Timestamp last_location_update;
    private double latitude;
    private double longitude;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public Timestamp getLast_location_update() {
        return last_location_update;
    }

    public void setLast_location_update(Timestamp last_location_update) {
        this.last_location_update = last_location_update;
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

    @NonNull
    @Override
    public String toString() {
        return "Username: "+getUsername()+
                "\n Email: " + getEmail()+
                "\n Last update: "+getLast_location_update()+
                "\n Latitude: "+getLatitude()+
                "\n Longitude: "+getLongitude();
    }
}
