package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Modèle pour la requête de mise à jour du statut d'un incident
 */
public class UpdateIncidentStatusRequest {
    
    @SerializedName("status")
    private String status;
    
    public UpdateIncidentStatusRequest(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
} 