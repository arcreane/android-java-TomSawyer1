package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Building {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("address")
    private String address;
    
    @SerializedName("code")
    private String code;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("apartments")
    private List<Apartment> apartments;
    
    @SerializedName("recent_incidents")
    private List<Incident> recentIncidents;
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Apartment> getApartments() {
        return apartments;
    }
    
    public void setApartments(List<Apartment> apartments) {
        this.apartments = apartments;
    }
    
    public List<Incident> getRecentIncidents() {
        return recentIncidents;
    }
    
    public void setRecentIncidents(List<Incident> recentIncidents) {
        this.recentIncidents = recentIncidents;
    }
} 