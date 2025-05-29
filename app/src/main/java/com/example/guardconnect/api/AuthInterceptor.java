package com.example.guardconnect.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Intercepteur pour ajouter des tokens d'authentification aux requêtes HTTP
 */
public class AuthInterceptor implements Interceptor {
    
    private static final String TAG = "AuthInterceptor";
    private static final String PREF_NAME = "GuardConnectPrefs";
    private String authToken;
    private SharedPreferences prefs;
    
    public AuthInterceptor(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public AuthInterceptor(Context context, String authToken) {
        this(context);
        this.authToken = authToken;
    }
    
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        if (prefs != null) {
            prefs.edit().putString("auth_token", authToken).apply();
        }
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Utiliser le token en mémoire ou celui des préférences
        String token = authToken;
        if (token == null && prefs != null) {
            token = prefs.getString("auth_token", null);
        }
        
        // Si pas de token, laisser passer la requête sans modification
        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }
        
        // Ajouter le token à l'en-tête Authorization
        Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        
        return chain.proceed(newRequest);
    }
} 