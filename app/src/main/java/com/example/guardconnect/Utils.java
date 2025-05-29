package com.example.guardconnect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Classe utilitaire pour centraliser les fonctions communes à plusieurs activités
 */
public class Utils {
    
    private static final String PREFS_NAME = "GuardConnectPrefs";
    
    /**
     * Configure la barre d'action avec un titre et un bouton retour ou la masque complètement
     */
    public static void setupActionBar(AppCompatActivity activity, String title) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            // Masquer complètement la barre d'action
            actionBar.hide();
        }
    }
    
    /**
     * Charge la langue enregistrée
     */
    public static void loadSavedLanguage(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", "fr"); // Français par défaut
        setLocale(activity, langCode);
    }
    
    /**
     * Change la locale de l'application
     */
    public static void setLocale(Activity activity, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());
    }
    
    /**
     * Enregistre la préférence de langue
     */
    public static void saveLanguagePreference(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", langCode);
        editor.apply();
    }
    
    /**
     * Vérifie si les notifications sont activées
     */
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("notifications_enabled", true);
    }
    
    /**
     * Définit l'état des notifications
     */
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }
} 