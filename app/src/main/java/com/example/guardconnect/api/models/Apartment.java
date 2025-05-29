package com.example.guardconnect.api.models;

import android.util.Log;
import com.google.gson.annotations.SerializedName;

public class Apartment {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("building_id")
    private int buildingId;
    
    @SerializedName("building_name")
    private String buildingName;
    
    @SerializedName("number")
    private String number;
    
    @SerializedName("floor")
    private Integer floor;
    
    @SerializedName("relation_type")
    private String relationType;
    
    // Getters et Setters
    public int getId() {
        Log.d("GuardConnect", "Apartment.getId(): " + id);
        return id;
    }
    
    public void setId(int id) {
        Log.d("GuardConnect", "Apartment.setId(): " + id);
        this.id = id;
    }
    
    public int getBuildingId() {
        return buildingId;
    }
    
    public void setBuildingId(int buildingId) {
        this.buildingId = buildingId;
    }
    
    public String getBuildingName() {
        return buildingName;
    }
    
    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
    
    public String getNumber() {
        Log.d("GuardConnect", "Apartment.getNumber(): " + number);
        return number;
    }
    
    public void setNumber(String number) {
        Log.d("GuardConnect", "Apartment.setNumber(): " + number);
        this.number = number;
    }
    
    public Integer getFloor() {
        return floor;
    }
    
    public void setFloor(Integer floor) {
        this.floor = floor;
    }
    
    public String getRelationType() {
        return relationType;
    }
    
    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
    
    public boolean isTenant() {
        return "tenant".equals(relationType);
    }
    
    public boolean isOwner() {
        return "owner".equals(relationType);
    }
    
    @Override
    public String toString() {
        return "Apt " + number + (floor != null ? " (Étage " + floor + ")" : "") + " - " + buildingName;
    }
} 