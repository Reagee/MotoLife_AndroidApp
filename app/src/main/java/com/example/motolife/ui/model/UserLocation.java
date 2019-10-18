package com.example.motolife.ui.model;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserLocation {
    private int id;
    private String username;
    private Timestamp last_location_update;
    private Float latitude;
    private Float longitude;
}
