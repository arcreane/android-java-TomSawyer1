package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle simplifié pour un utilisateur
 */
public class User {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("fullname")
    private String fullName;
    
    @SerializedName("role")
    private String role;
    
    // Getters et Setters essentiels
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
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Indique si l'utilisateur est un gardien
     */
    public boolean isGuardian() {
        return "guardian".equals(role);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
} 