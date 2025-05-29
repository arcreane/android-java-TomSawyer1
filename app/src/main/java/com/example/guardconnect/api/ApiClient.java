package com.example.guardconnect.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.guardconnect.api.models.Apartment;
import com.example.guardconnect.api.models.Building;
import com.example.guardconnect.api.models.Incident;
import com.example.guardconnect.api.models.User;
import com.example.guardconnect.api.models.UpdateIncidentStatusRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.MultipartBody;

public class ApiClient {
    
    // Utiliser l'adresse spéciale pour l'émulateur Android
    public static final String BASE_URL = "http://10.0.2.2:3000/api/";
    // Adresse IP directe de la machine hôte (PC) - pour les appareils physiques
    // public static final String BASE_URL = "http://192.168.1.58:3000/api/";
    private static final String PREF_NAME = "GuardConnectPrefs";
    private static final String KEY_USER = "user_data";
    
    private static ApiClient instance;
    private final ApiService apiService;
    private final SharedPreferences preferences;
    private final Context context;
    private User currentUser;
    
    private static final int TIMEOUT = 30000; // 30 secondes
    
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        // Configurer le logging des requêtes
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d("ApiClient", "HTTP Request/Response: " + message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Configurer l'intercepteur d'authentification
        AuthInterceptor authInterceptor = new AuthInterceptor(this.context);
        
        // Configurer OkHttpClient avec les paramètres de base et un proxy
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor)
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .dns(hostname -> {
                    Log.d("ApiClient", "Résolution DNS pour: " + hostname);
                    if (hostname.equals("192.168.1.58")) {
                        return Collections.singletonList(InetAddress.getByName("10.0.2.2"));
                    }
                    return Collections.singletonList(InetAddress.getByName(hostname));
                })
                .build();
        
        Log.d("ApiClient", "Initialisation avec BASE_URL: " + BASE_URL);
        
        // Configurer Gson pour gérer les dates
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        
        // Créer l'instance Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        // Créer l'API service
        apiService = retrofit.create(ApiService.class);
        
        // Initialiser les préférences
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Charger l'utilisateur en cache
        String userJson = preferences.getString(KEY_USER, null);
        if (userJson != null) {
            currentUser = gson.fromJson(userJson, User.class);
        }
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Récupère l'instance existante de ApiClient.
     * Note: getInstance(Context) doit être appelé au moins une fois avant d'utiliser cette méthode.
     * @return L'instance existante d'ApiClient ou null si aucune instance n'a été créée
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            return null;
        }
        return instance;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Met à jour le statut d'un incident
     * @param incidentId ID de l'incident à mettre à jour
     * @param status Nouveau statut pour l'incident
     * @param callback Callback pour recevoir la réponse
     */
    public static void updateIncidentStatus(int incidentId, String status, final Callback<Incident> callback) {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(status);
        ApiService service = getInstance().getApiService();
        Call<Incident> call = service.updateIncidentStatus(incidentId, request);
        
        call.enqueue(new Callback<Incident>() {
            @Override
            public void onResponse(Call<Incident> call, Response<Incident> response) {
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Incident> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
    
    /**
     * Récupère un incident par son code de suivi
     * @param trackingCode Le code de suivi unique de l'incident
     * @param callback Le callback pour recevoir le résultat
     */
    public void getIncidentByTrackingCode(String trackingCode, Callback<Incident> callback) {
        try {
            apiService.getIncidentByTrackingCode(trackingCode).enqueue(callback);
        } catch (Exception e) {
            throw e;
        }
    }
    
    /**
     * Vérifie la connexion au serveur backend
     */
    public void checkConnection(final Callback<Void> callback) {
        Log.d("ApiClient", "Tentative de connexion à l'API...");
        apiService.checkConnection().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("ApiClient", "Réponse reçue du serveur: " + response.code());
                if (response.isSuccessful()) {
                    Log.d("ApiClient", "Connexion réussie!");
                } else {
                    Log.e("ApiClient", "Erreur de connexion: " + response.code());
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("ApiClient", "Échec de la connexion", t);
                Log.e("ApiClient", "Message d'erreur: " + t.getMessage());
                Log.e("ApiClient", "Type d'erreur: " + t.getClass().getName());
                callback.onFailure(call, t);
            }
        });
    }
    
    /**
     * Log pour vérifier si les appartements sont inclus dans les bâtiments
     */
    private static void logBuildingData(List<Building> buildings) {
        if (buildings == null) {
            Log.d("GuardConnect", "Liste des bâtiments est NULL!");
            return;
        }
        
        Log.d("GuardConnect", "Nombre de bâtiments chargés: " + buildings.size());
        
        for (Building building : buildings) {
            Log.d("GuardConnect", "Bâtiment ID: " + building.getId() + ", Nom: " + building.getName());
            
            List<Apartment> apartments = building.getApartments();
            if (apartments != null) {
                Log.d("GuardConnect", "  Le bâtiment a " + apartments.size() + " appartements");
                for (Apartment apt : apartments) {
                    Log.d("GuardConnect", "  Appartement: ID=" + apt.getId() + ", Numéro=" + apt.getNumber() + ", Étage=" + apt.getFloor());
                }
            } else {
                Log.d("GuardConnect", "  Le bâtiment n'a PAS d'appartements (null)");
            }
        }
    }
    
    /**
     * Récupère les appartements d'un bâtiment spécifique
     * 
     * @param buildingId l'ID du bâtiment
     * @param callback le callback pour la réponse
     */
    public void getBuildingApartments(int buildingId, final Callback<List<Apartment>> callback) {
        apiService.getBuildingApartments(buildingId).enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Apartment> apartments = response.body();
                    callback.onResponse(call, response);
                } else {
                    callback.onResponse(call, response);
                }
            }

            @Override
            public void onFailure(Call<List<Apartment>> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
    
    public ApiService getApiService() {
        return apiService;
    }

    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .addInterceptor(new AuthInterceptor(context))
            .build();
    }

    public void createIncident(Map<String, Object> incidentData, okhttp3.Callback callback) {
        try {
            // Vérifier les données requises
            if (!incidentData.containsKey("title") || !incidentData.containsKey("description")) {
                throw new IllegalArgumentException("Les champs titre et description sont obligatoires");
            }

            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", (String) incidentData.get("title"))
                .addFormDataPart("description", (String) incidentData.get("description"))
                .addFormDataPart("location", (String) incidentData.get("location"))
                .addFormDataPart("email", (String) incidentData.get("email"))
                .addFormDataPart("reported_by", String.valueOf(incidentData.get("reported_by")))
                .build();

            Request request = new Request.Builder()
                .url(BASE_URL + "incidents/")
                .post(requestBody)
                .build();

            getClient().newCall(request).enqueue(callback);
        } catch (Exception e) {
            callback.onFailure(null, new IOException("Erreur lors de la création de l'incident: " + e.getMessage()));
        }
    }
} 