package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("imagePath")
    private String imagePath;
    
    @SerializedName("fileName")
    private String fileName;
    
    @SerializedName("size")
    private long size;
    
    // Getters et Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
} 