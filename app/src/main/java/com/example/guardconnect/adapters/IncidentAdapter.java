package com.example.guardconnect.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guardconnect.IncidentDetailActivity;
import com.example.guardconnect.R;
import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.Incident;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private static final String TAG = "IncidentAdapter";
    private final List<Incident> incidentList;
    private final Context context;
    private final boolean isGuardian;
    private static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault());

    public IncidentAdapter(List<Incident> incidentList, Context context, boolean isGuardian) {
        this.incidentList = incidentList;
        this.context = context;
        this.isGuardian = isGuardian;
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new IncidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        Incident incident = incidentList.get(position);
        
        // Définir le titre et l'emplacement
        holder.tvTitle.setText(incident.getTitle());
        holder.tvLocation.setText("Emplacement: " + incident.getLocation());
        
        // Formater et définir la date
        String formattedDate = formatDate(incident.getCreatedAt());
        holder.tvDate.setText("Signalé le: " + formattedDate);
        
        // Définir le code de suivi
        holder.tvTrackingCode.setText("Code: " + incident.getTrackingCode());
        
        // Définir et styliser le statut
        holder.tvStatus.setText(getStatusText(incident.getStatus()));
        setStatusBackground(holder.tvStatus, incident.getStatus());
        
        // Si c'est un gardien, permettre de cliquer sur le statut pour le changer
        if (isGuardian) {
            holder.tvStatus.setOnClickListener(v -> showStatusChangeDialog(incident, holder.getAdapterPosition()));
        }
        
        // Configurer le clic sur l'élément pour ouvrir les détails
        holder.itemView.setOnClickListener(v -> openIncidentDetails(incident));
    }

    @Override
    public int getItemCount() {
        return incidentList.size();
    }
    
    private String formatDate(String dateString) {
        try {
            if (dateString != null) {
                Date date = INPUT_FORMAT.parse(dateString);
                if (date != null) {
                    return OUTPUT_FORMAT.format(date);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Date inconnue";
    }
    
    private String getStatusText(String status) {
        if (status == null) return "En attente";
        
        switch (status.toLowerCase()) {
            case "pending":
                return "Non lu";
            case "in_progress":
                return "En cours";
            case "resolved":
                return "Résolu";
            case "closed":
                return "Clôturé";
            default:
                return status;
        }
    }
    
    private void setStatusBackground(TextView textView, String status) {
        int backgroundResource;
        
        if (status == null) {
            backgroundResource = R.drawable.status_background;
            textView.setBackground(ContextCompat.getDrawable(context, backgroundResource));
            return;
        }
        
        switch (status.toLowerCase()) {
            case "in_progress":
                backgroundResource = R.drawable.status_background_in_progress;
                break;
            case "resolved":
                backgroundResource = R.drawable.status_background_resolved;
                break;
            case "closed":
                backgroundResource = R.drawable.status_background_closed;
                break;
            case "pending":
            default:
                backgroundResource = R.drawable.status_background;
                break;
        }
        
        textView.setBackground(ContextCompat.getDrawable(context, backgroundResource));
    }
    
    /**
     * Affiche une boîte de dialogue pour changer le statut d'un incident
     */
    private void showStatusChangeDialog(Incident incident, int position) {
        final String[] statusOptions = {"Non lu", "En cours", "Résolu", "Clôturé"};
        final String[] statusValues = {"pending", "in_progress", "resolved", "closed"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Changer le statut");
        builder.setItems(statusOptions, (dialog, which) -> {
            String newStatus = statusValues[which];
            updateIncidentStatus(incident, newStatus, position);
        });
        builder.show();
    }
    
    /**
     * Met à jour le statut d'un incident via l'API
     */
    private void updateIncidentStatus(Incident incident, String newStatus, int position) {
        ApiClient.updateIncidentStatus(incident.getId(), newStatus, new Callback<Incident>() {
            @Override
            public void onResponse(Call<Incident> call, Response<Incident> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Mettre à jour l'incident dans la liste
                    Incident updatedIncident = response.body();
                    incident.setStatus(updatedIncident.getStatus());
                    notifyItemChanged(position);
                    
                    Toast.makeText(context, "Statut mis à jour avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Erreur lors de la mise à jour du statut: " + response.code());
                    Toast.makeText(context, "Erreur lors de la mise à jour du statut", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Incident> call, Throwable t) {
                Log.e(TAG, "Échec de la requête: " + t.getMessage());
                Toast.makeText(context, "Impossible de se connecter au serveur", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Ouvre l'activité de détail de l'incident
     */
    private void openIncidentDetails(Incident incident) {
        Intent intent = new Intent(context, IncidentDetailActivity.class);
        intent.putExtra("incident_id", incident.getId());
        intent.putExtra("is_guardian", isGuardian);
        context.startActivity(intent);
    }
    
    public void updateIncidents(List<Incident> newIncidents) {
        this.incidentList.clear();
        this.incidentList.addAll(newIncidents);
        notifyDataSetChanged();
    }
    
    static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvLocation;
        TextView tvDate;
        TextView tvStatus;
        TextView tvTrackingCode;
        
        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_incident_title);
            tvLocation = itemView.findViewById(R.id.tv_incident_location);
            tvDate = itemView.findViewById(R.id.tv_incident_date);
            tvStatus = itemView.findViewById(R.id.tv_incident_status);
            tvTrackingCode = itemView.findViewById(R.id.tv_tracking_code);
        }
    }
} 