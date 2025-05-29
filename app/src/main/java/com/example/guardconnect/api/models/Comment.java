package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle pour un commentaire sur un incident
 */
public class Comment {
    
    @SerializedName("id")
    private int id;
    
    @SerializedName("incident_id")
    private int incidentId;
    
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("user_name")
    private String userName;
    
    @SerializedName("comment")
    private String comment;
    
    @SerializedName("created_at")
    private String createdAt;
    
    // Getters et Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getIncidentId() {
        return incidentId;
    }
    
    public void setIncidentId(int incidentId) {
        this.incidentId = incidentId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", incidentId=" + incidentId +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", comment='" + comment + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
} 