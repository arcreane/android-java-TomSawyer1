package com.example.guardconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private Button btnDeclareIncident;
    private Button btnGuardianAccess;
    private Button btnTrackIncident;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée au démarrage
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("GuardConnect");

        // Initialiser les boutons
        btnDeclareIncident = findViewById(R.id.btn_declare_incident);
        btnGuardianAccess = findViewById(R.id.btn_guardian_access);
        btnTrackIncident = findViewById(R.id.btn_track_incident);

        // Configurer les écouteurs de clics
        btnDeclareIncident.setOnClickListener(view -> {
            Intent formIntent = new Intent(MainActivity.this, FormActivity.class);
            startActivity(formIntent);
        });

        btnGuardianAccess.setOnClickListener(view -> {
            Intent guardianIntent = new Intent(MainActivity.this, GuardianLoginActivity.class);
            startActivity(guardianIntent);
        });

        btnTrackIncident.setOnClickListener(view -> {
            Intent trackIntent = new Intent(MainActivity.this, FollowActivity.class);
            startActivity(trackIntent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Méthode pour afficher la boîte de dialogue des paramètres
    private void showSettingsDialog() {
        final CharSequence[] options = {"Changer thème", "Langue (Language)", "Contact"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paramètres");
        builder.setItems(options, (dialog, item) -> {
            if (item == 0) {
                // Option thème
                toggleTheme();
            } else if (item == 1) {
                // Option Langue
                showLanguageOptions();
            } else if (item == 2) {
                // Option Contact
                showContactInfo();
            }
        });
        builder.show();
    }
    
    // Basculer entre thème clair et sombre
    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        
        // Inverser le thème
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean("dark_mode", false).apply();
            Toast.makeText(this, "Mode clair activé", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            prefs.edit().putBoolean("dark_mode", true).apply();
            Toast.makeText(this, "Mode sombre activé", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Affiche les options de langue
    private void showLanguageOptions() {
        final CharSequence[] languages = {"Français", "English"};
        
        // Récupérer la langue actuelle
        SharedPreferences prefs = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE);
        String currentLang = prefs.getString("language", "fr");
        int defaultSelection = currentLang.equals("fr") ? 0 : 1;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choisir la langue / Choose language");
        builder.setSingleChoiceItems(languages, defaultSelection, (dialog, which) -> {
            // Enregistrer la sélection
            String langCode = (which == 0) ? "fr" : "en";
            Utils.saveLanguagePreference(this, langCode);
            Utils.setLocale(this, langCode);
            dialog.dismiss();
            
            // Redémarrer l'activité pour appliquer le changement
            Intent refresh = new Intent(this, MainActivity.class);
            refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(refresh);
            finish();
        });
        builder.show();
    }
    
    // Affiche les informations de contact
    private void showContactInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact");
        builder.setMessage("GuardConnect\n\nEmail: support@guardconnect.com\nTéléphone: +33 1 23 45 67 89");
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}
