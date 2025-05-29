package com.example.guardconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.Incident;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowActivity extends AppCompatActivity {

    private static final String TAG = "FollowActivity";
    private EditText etTrackingCode;
    private Button btnTrack;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_follow);

        // Activer le bouton retour dans la barre d'action
        Utils.setupActionBar(this, "Suivi des incidents");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.follow), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialiser les vues
        etTrackingCode = findViewById(R.id.nincident);
        btnTrack = findViewById(R.id.btn_track);
        progressBar = findViewById(R.id.progress_bar);
        
        // Configurer le bouton de suivi
        btnTrack.setOnClickListener(v -> trackIncident());
    }
    
    /**
     * Recherche un incident par son code de suivi
     */
    private void trackIncident() {
        String trackingCode = etTrackingCode.getText().toString().trim();
        
        if (trackingCode.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un code de suivi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Afficher l'indicateur de chargement
        progressBar.setVisibility(View.VISIBLE);
        btnTrack.setEnabled(false);
        
        // Rechercher l'incident avec le code de suivi
        ApiClient.getInstance(this).getApiService().getIncidentByTrackingCode(trackingCode)
            .enqueue(new Callback<Incident>() {
                @Override
                public void onResponse(Call<Incident> call, Response<Incident> response) {
                    progressBar.setVisibility(View.GONE);
                    btnTrack.setEnabled(true);
                    
                    if (response.isSuccessful() && response.body() != null) {
                        Incident incident = response.body();
                        // Ouvrir l'écran de détail pour cet incident
                        openIncidentDetails(incident);
                    } else {
                        Toast.makeText(FollowActivity.this, "Aucun incident trouvé avec ce code de suivi", 
                                Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<Incident> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnTrack.setEnabled(true);
                    Toast.makeText(FollowActivity.this, "Impossible de se connecter au serveur", 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    /**
     * Ouvre l'écran de détail pour un incident
     */
    private void openIncidentDetails(Incident incident) {
        Intent intent = new Intent(this, IncidentDetailActivity.class);
        intent.putExtra("incident_id", incident.getId());
        intent.putExtra("is_guardian", false); // C'est un suivi par un locataire
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}