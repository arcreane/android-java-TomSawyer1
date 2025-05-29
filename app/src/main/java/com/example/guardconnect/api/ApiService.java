package com.example.guardconnect.api;

import com.example.guardconnect.api.models.Apartment;
import com.example.guardconnect.api.models.Building;
import com.example.guardconnect.api.models.Comment;
import com.example.guardconnect.api.models.Incident;
import com.example.guardconnect.api.models.ImageUploadResponse;
import com.example.guardconnect.api.models.LoginRequest;
import com.example.guardconnect.api.models.LoginResponse;
import com.example.guardconnect.api.models.UpdateIncidentStatusRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import okhttp3.MultipartBody;

/**
 * Interface Retrofit pour les appels API
 * Simplifiée pour ne garder que l'essentiel
 */
public interface ApiService {
    
    // Récupérer la liste des bâtiments (utilisé pour vérifier la connexion)
    @GET("buildings")
    Call<List<Building>> getBuildings();
    
    // Récupérer tous les incidents
    @GET("incidents")
    Call<List<Incident>> getAllIncidents();
    
    // Récupérer un incident par son code de suivi
    @GET("incidents/track/{code}")
    Call<Incident> getIncidentByTrackingCode(@Path("code") String trackingCode);
    
    // Récupérer un incident par son ID avec ses commentaires
    @GET("incidents/{id}")
    Call<Incident> getIncidentDetails(@Path("id") int id);

    // Authentification du gardien avec son code secret
    @POST("auth/guardian")
    Call<LoginResponse> loginGuardian(@Body LoginRequest loginRequest);

    // Récupérer les incidents
    @GET("incidents")
    Call<List<Incident>> getIncidents();

    // Récupérer les incidents par code de suivi
    @GET("incidents")
    Call<List<Incident>> getIncidentsByTrackingCode(@Query("trackingCode") String trackingCode);

    // Mettre à jour le statut d'un incident
    @PUT("incidents/{id}")
    Call<Incident> updateIncidentStatus(@Path("id") int id, @Body UpdateIncidentStatusRequest request);
    
    // Créer un incident
    @POST("incidents")
    Call<Incident> createIncident(@Body Map<String, Object> incident);
    
    // Ajouter un commentaire à un incident
    @POST("incidents/{id}/comments")
    Call<Comment> addComment(@Path("id") int incidentId, @Body Map<String, Object> comment);

    // Récupérer la liste des appartements d'un bâtiment
    @GET("apartments")
    Call<List<Apartment>> getBuildingApartments(@Query("building_id") int buildingId);
    
    // Télécharger une image d'incident
    @Multipart
    @POST("uploads/incident-image")
    Call<ImageUploadResponse> uploadIncidentImage(@Part MultipartBody.Part image);

    // Vérifier la connexion au serveur
    @GET("health")
    Call<Void> checkConnection();

    // Récupérer les commentaires d'un incident
    @GET("incidents/{id}/comments")
    Call<List<Comment>> getIncidentComments(@Path("id") int incidentId);
} 