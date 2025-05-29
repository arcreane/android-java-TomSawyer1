package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Classe représentant une demande d'authentification
 */
public class LoginRequest {
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("password")
    private String password;
    
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
} 