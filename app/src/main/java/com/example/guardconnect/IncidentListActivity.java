package com.example.guardconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.guardconnect.adapters.IncidentAdapter;
import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.Incident;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentListActivity extends AppCompatActivity {

    private static final String TAG = "IncidentListActivity";
    private RecyclerView recyclerView;
    private IncidentAdapter adapter;
    private List<Incident> incidentList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isGuardian;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_list);

        // Initialiser l'ApiClient avec le contexte
        apiClient = ApiClient.getInstance(this);

        // Récupérer l'intention et vérifier si l'utilisateur est un gardien
        isGuardian = getIntent().getBooleanExtra("is_guardian", false);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(isGuardian ? "Liste des incidents" : "Vos incidents");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialiser les vues
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        // Configurer le RecyclerView
        setupRecyclerView();

        // Configurer la fonction de rafraîchissement
        swipeRefreshLayout.setOnRefreshListener(this::loadIncidents);

        // Charger les incidents
        loadIncidents();
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new IncidentAdapter(new ArrayList<>(), this, isGuardian);
        recyclerView.setAdapter(adapter);
    }

    private void loadIncidents() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        // Récupérer l'ID du gardien si l'utilisateur est un gardien
        String guardianId = null;
        if (isGuardian) {
            guardianId = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE)
                    .getString("guardian_id", null);
        }

        // Créer la requête appropriée en fonction du type d'utilisateur
        Call<List<Incident>> call;
        if (isGuardian && guardianId != null) {
            // Pour les gardiens, récupérer tous les incidents
            call = apiClient.getApiService().getAllIncidents();
        } else {
            // Pour les utilisateurs normaux, récupérer tous les incidents
            // Nous filtrerons ensuite localement
            call = apiClient.getApiService().getAllIncidents();
        }

        call.enqueue(new Callback<List<Incident>>() {
            @Override
            public void onResponse(Call<List<Incident>> call, Response<List<Incident>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    incidentList.clear();
                    
                    // Si l'utilisateur n'est pas un gardien, filtrer les incidents
                    if (!isGuardian) {
                        // Récupérer l'ID de l'utilisateur connecté
                        int userId = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE)
                                .getInt("user_id", -1);
                        
                        // Filtrer les incidents pour ne garder que ceux de l'utilisateur
                        for (Incident incident : response.body()) {
                            if (incident.getReportedBy() == userId) {
                                incidentList.add(incident);
                            }
                        }
                    } else {
                        // Pour les gardiens, afficher tous les incidents
                        incidentList.addAll(response.body());
                    }
                    
                    // Mettre à jour l'adaptateur avec la nouvelle liste
                    adapter.updateIncidents(incidentList);

                    if (incidentList.isEmpty()) {
                        showEmptyState();
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<Incident>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        if (incidentList.isEmpty()) {
            tvEmptyState.setText(isGuardian
                    ? "Aucun incident n'a été signalé."
                    : "Vous n'avez signalé aucun incident.");
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            incidentList.clear();
            adapter.notifyDataSetChanged();
        }
        recyclerView.setAdapter(null);
        recyclerView.setDrawingCacheEnabled(false);
    }
} 