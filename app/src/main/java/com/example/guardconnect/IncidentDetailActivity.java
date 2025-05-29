package com.example.guardconnect;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.guardconnect.adapters.CommentAdapter;
import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.Comment;
import com.example.guardconnect.api.models.Incident;
import com.example.guardconnect.api.models.UpdateIncidentStatusRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentDetailActivity extends AppCompatActivity {
    
    private static final String TAG = "IncidentDetailActivity";
    
    // Constantes pour les statuts
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_RESOLVED = "resolved";
    
    // Views
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvLocation;
    private TextView tvTrackingCode;
    private TextView tvDescription;
    private ImageView imgIncident;
    private TextView tvCurrentStatus;
    private TextView tvCurrentStatusSmall;
    private RadioGroup radioGroupStatus;
    private RadioButton radioPending;
    private RadioButton radioInProgress;
    private RadioButton radioResolved;
    private Button btnUpdateStatus;
    private RecyclerView recyclerComments;
    private TextView tvNoComments;
    private EditText etComment;
    private Button btnAddComment;
    private ProgressBar progressBar;
    private CardView cardStatusManagement;
    private View commentInputContainer;
    
    // Données
    private int incidentId;
    private Incident currentIncident;
    private List<Comment> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private int currentUserId;
    private boolean isGuardian;
    
    // Formats de date
    private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault());
    
    private ApiClient apiClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_detail);
        
        // Récupérer l'ID de l'incident depuis l'intent
        if (getIntent().hasExtra("incident_id")) {
            incidentId = getIntent().getIntExtra("incident_id", -1);
        } else {
            Toast.makeText(this, "Erreur: ID d'incident manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Vérifier si l'utilisateur est un gardien
        isGuardian = getIntent().getBooleanExtra("is_guardian", false);
        
        // Récupérer l'ID de l'utilisateur connecté depuis les préférences
        currentUserId = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE)
                .getInt("user_id", 0);
        
        // Initialiser les vues
        initViews();
        
        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Détail de l'incident");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Configurer le RecyclerView pour les commentaires
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList, this);
        recyclerComments.setAdapter(commentAdapter);
        
        // Configurer les écouteurs
        setupListeners();
        
        // Charger les détails de l'incident
        apiClient = ApiClient.getInstance(this);
        loadIncidentDetails();
    }
    
    private void initViews() {
        tvTitle = findViewById(R.id.tv_incident_title);
        tvDate = findViewById(R.id.tv_incident_date);
        tvLocation = findViewById(R.id.tv_incident_location);
        tvTrackingCode = findViewById(R.id.tv_tracking_code);
        tvDescription = findViewById(R.id.tv_incident_description);
        imgIncident = findViewById(R.id.img_incident);
        tvCurrentStatus = findViewById(R.id.tv_current_status);
        tvCurrentStatusSmall = findViewById(R.id.tv_current_status_small);
        radioGroupStatus = findViewById(R.id.radio_group_status);
        radioPending = findViewById(R.id.radio_pending);
        radioInProgress = findViewById(R.id.radio_in_progress);
        radioResolved = findViewById(R.id.radio_resolved);
        btnUpdateStatus = findViewById(R.id.btn_update_status);
        recyclerComments = findViewById(R.id.recycler_comments);
        tvNoComments = findViewById(R.id.tv_no_comments);
        etComment = findViewById(R.id.et_comment);
        btnAddComment = findViewById(R.id.btn_add_comment);
        progressBar = findViewById(R.id.progress_bar);
        cardStatusManagement = findViewById(R.id.card_status_management);
        commentInputContainer = findViewById(R.id.comment_input_container);
        
        // Masquer les éléments de gestion si l'utilisateur n'est pas un gardien
        if (!isGuardian) {
            cardStatusManagement.setVisibility(View.GONE);
            commentInputContainer.setVisibility(View.GONE);
        }
    }
    
    private void setupListeners() {
        // Bouton de mise à jour du statut
        btnUpdateStatus.setOnClickListener(v -> updateIncidentStatus());
        
        // Bouton d'ajout de commentaire
        btnAddComment.setOnClickListener(v -> addComment());
    }
    
    private void loadIncidentDetails() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiClient.getApiService().getIncidentDetails(incidentId).enqueue(new Callback<Incident>() {
            @Override
            public void onResponse(Call<Incident> call, Response<Incident> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    currentIncident = response.body();
                    displayIncidentDetails();
                    loadComments();
                } else {
                    Toast.makeText(IncidentDetailActivity.this, 
                            "Erreur lors du chargement des détails de l'incident", 
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onFailure(Call<Incident> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(IncidentDetailActivity.this, 
                        "Impossible de charger les détails de l'incident", 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    private void loadComments() {
        if (currentIncident == null) return;
        
        apiClient.getApiService().getIncidentComments(incidentId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentList.clear();
                    commentList.addAll(response.body());
                    commentAdapter.notifyDataSetChanged();
                    
                    // Afficher ou masquer le message "Aucun commentaire"
                    tvNoComments.setVisibility(commentList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(IncidentDetailActivity.this, 
                            "Erreur lors du chargement des commentaires", 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Toast.makeText(IncidentDetailActivity.this, 
                        "Impossible de charger les commentaires", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void displayIncidentDetails() {
        if (currentIncident == null) return;
        
        // Remplir les détails de l'incident
        tvTitle.setText(currentIncident.getTitle());
        tvDate.setText("Signalé le: " + formatDate(currentIncident.getCreatedAt()));
        tvLocation.setText("Emplacement: " + currentIncident.getLocation());
        tvTrackingCode.setText("Code de suivi: " + currentIncident.getTrackingCode());
        tvDescription.setText(currentIncident.getDescription());
        
        // Afficher l'image si disponible
        if (currentIncident.getImagePath() != null && !currentIncident.getImagePath().isEmpty()) {
            imgIncident.setVisibility(View.VISIBLE);
            // Utiliser Glide pour charger l'image
            Glide.with(this)
                    .load(currentIncident.getImagePath())
                    .centerCrop()
                    .into(imgIncident);
        } else {
            imgIncident.setVisibility(View.GONE);
        }
        
        // Définir le statut actuel
        String statusText = getStatusText(currentIncident.getStatus());
        tvCurrentStatus.setText(statusText);
        tvCurrentStatusSmall.setText(statusText);
        setStatusBackground(tvCurrentStatus, currentIncident.getStatus());
        setStatusBackground(tvCurrentStatusSmall, currentIncident.getStatus());
        
        // Pré-sélectionner le statut actuel dans le groupe radio (pour les gardiens)
        if (isGuardian) {
            switch (currentIncident.getStatus()) {
                case STATUS_PENDING:
                    radioPending.setChecked(true);
                    break;
                case STATUS_IN_PROGRESS:
                    radioInProgress.setChecked(true);
                    break;
                case STATUS_RESOLVED:
                    radioResolved.setChecked(true);
                    break;
            }
        }
    }
    
    private void updateIncidentStatus() {
        int selectedId = radioGroupStatus.getCheckedRadioButtonId();
        String newStatus;
        
        if (selectedId == R.id.radio_pending) {
            newStatus = STATUS_PENDING;
        } else if (selectedId == R.id.radio_in_progress) {
            newStatus = STATUS_IN_PROGRESS;
        } else if (selectedId == R.id.radio_resolved) {
            newStatus = STATUS_RESOLVED;
        } else {
            Toast.makeText(this, "Veuillez sélectionner un statut", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Vérifier si le statut est déjà le même
        if (newStatus.equals(currentIncident.getStatus())) {
            Toast.makeText(this, "Le statut est déjà " + getStatusText(newStatus), Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnUpdateStatus.setEnabled(false);
        
        // Appeler l'API pour mettre à jour le statut
        ApiClient.getInstance(this).getApiService()
                .updateIncidentStatus(incidentId, new UpdateIncidentStatusRequest(newStatus))
                .enqueue(new Callback<Incident>() {
                    @Override
                    public void onResponse(Call<Incident> call, Response<Incident> response) {
                        progressBar.setVisibility(View.GONE);
                        btnUpdateStatus.setEnabled(true);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            Incident updatedIncident = response.body();
                            currentIncident.setStatus(updatedIncident.getStatus());
                            displayIncidentDetails();
                            
                            Toast.makeText(IncidentDetailActivity.this, "Statut mis à jour avec succès", Toast.LENGTH_SHORT).show();
                            
                            // Ajouter automatiquement un commentaire indiquant le changement de statut
                            String commentText = "Le statut a été changé à " + getStatusText(newStatus);
                            addSystemComment(commentText);
                        } else {
                            Toast.makeText(IncidentDetailActivity.this, "Erreur lors de la mise à jour du statut", Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<Incident> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnUpdateStatus.setEnabled(true);
                        Toast.makeText(IncidentDetailActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void addComment() {
        String commentText = etComment.getText().toString().trim();
        
        if (TextUtils.isEmpty(commentText)) {
            etComment.setError("Veuillez saisir un commentaire");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnAddComment.setEnabled(false);
        
        // Préparer les données pour l'API
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("user_id", currentUserId);
        commentData.put("comment", commentText);
        
        // Appeler l'API pour ajouter le commentaire
        apiClient.getApiService()
                .addComment(incidentId, commentData)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        progressBar.setVisibility(View.GONE);
                        btnAddComment.setEnabled(true);
                        
                        if (response.isSuccessful() && response.body() != null) {
                            Comment newComment = response.body();
                            
                            // Ajouter le commentaire à la liste
                            if (currentIncident.getComments() == null) {
                                currentIncident.setComments(new ArrayList<>());
                            }
                            currentIncident.getComments().add(0, newComment);
                            commentList.add(0, newComment);
                            commentAdapter.notifyItemInserted(0);
                            
                            // Effacer le champ de texte
                            etComment.setText("");
                            
                            // Masquer le message "Aucun commentaire"
                            tvNoComments.setVisibility(View.GONE);
                            
                            Toast.makeText(IncidentDetailActivity.this, "Commentaire ajouté", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(IncidentDetailActivity.this, "Erreur lors de l'ajout du commentaire", Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnAddComment.setEnabled(true);
                        Toast.makeText(IncidentDetailActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void addSystemComment(String commentText) {
        // Préparer les données pour l'API
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("user_id", currentUserId);
        commentData.put("comment", commentText);
        
        // Appeler l'API pour ajouter le commentaire système
        apiClient.getApiService()
                .addComment(incidentId, commentData)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment newComment = response.body();
                            
                            // Ajouter le commentaire à la liste
                            if (currentIncident.getComments() == null) {
                                currentIncident.setComments(new ArrayList<>());
                            }
                            currentIncident.getComments().add(0, newComment);
                            commentList.add(0, newComment);
                            commentAdapter.notifyItemInserted(0);
                            
                            // Masquer le message "Aucun commentaire"
                            tvNoComments.setVisibility(View.GONE);
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        // Ignorer l'erreur, ce n'est pas critique
                    }
                });
    }
    
    private String formatDate(String dateString) {
        try {
            if (dateString != null) {
                // Essaye d'abord le format SQL classique
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString);
                    if (date != null) return OUTPUT_FORMAT.format(date);
                } catch (ParseException ignore) {}
                // Puis le format ISO
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(dateString);
                    if (date != null) return OUTPUT_FORMAT.format(date);
                } catch (ParseException ignore) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Date inconnue";
    }
    
    private String getStatusText(String status) {
        if (status == null) return "En attente";
        
        switch (status) {
            case STATUS_PENDING:
                return "Non lu";
            case STATUS_IN_PROGRESS:
                return "En cours";
            case STATUS_RESOLVED:
                return "Résolu";
            default:
                return status;
        }
    }
    
    private void setStatusBackground(TextView textView, String status) {
        int backgroundResource;
        
        if (status == null) {
            backgroundResource = R.drawable.status_background;
            textView.setBackgroundResource(backgroundResource);
            return;
        }
        
        switch (status) {
            case STATUS_IN_PROGRESS:
                backgroundResource = R.drawable.status_background_in_progress;
                break;
            case STATUS_RESOLVED:
                backgroundResource = R.drawable.status_background_resolved;
                break;
            case STATUS_PENDING:
            default:
                backgroundResource = R.drawable.status_background;
                break;
        }
        
        textView.setBackgroundResource(backgroundResource);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 