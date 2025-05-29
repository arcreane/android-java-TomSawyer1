package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Réponse du serveur après une tentative de connexion
 */
public class LoginResponse {
    
    @SerializedName("user")
    private User user;
    
    @SerializedName("auth_token")
    private String authToken;
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    
    @Override
    public String toString() {
        return "LoginResponse{" +
                "user=" + user +
                ", authToken='" + authToken + '\'' +
                '}';
    }
} 