package com.example.guardconnect.api.models;

import com.google.gson.annotations.SerializedName;

/**
 * Classe représentant la réponse du serveur lors de l'upload d'une image
 */
public class ImageUploadResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("imagePath")
    private String imagePath;

    @SerializedName("fileName")
    private String fileName;

    @SerializedName("size")
    private long size;

    /**
     * Constructeur par défaut
     */
    public ImageUploadResponse() {
    }

    /**
     * Constructeur avec paramètres
     */
    public ImageUploadResponse(boolean success, String imagePath, String fileName, long size) {
        this.success = success;
        this.imagePath = imagePath;
        this.fileName = fileName;
        this.size = size;
    }

    /**
     * Getters et setters
     */
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

    @Override
    public String toString() {
        return "ImageUploadResponse{" +
                "success=" + success +
                ", imagePath='" + imagePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", size=" + size +
                '}';
    }
} 