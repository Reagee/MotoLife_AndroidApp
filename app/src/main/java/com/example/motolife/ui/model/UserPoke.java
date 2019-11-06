package com.example.motolife.ui.model;

public class UserPoke {
    private String username;
    private String userToPoke;

    public UserPoke(String username, String userToPoke) {
        this.username = username;
        this.userToPoke = userToPoke;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserToPoke() {
        return userToPoke;
    }

    public void setUserToPoke(String userToPoke) {
        this.userToPoke = userToPoke;
    }
}
