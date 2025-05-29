package com.example.guardconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.LoginRequest;
import com.example.guardconnect.api.models.LoginResponse;
import com.example.guardconnect.api.models.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuardianLoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian_login);

        // Configurer la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Accès Gardien");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialiser les vues
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        // Configurer le bouton de connexion
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Tente de connecter le gardien
     */
    private void attemptLogin() {
        // Réinitialiser les erreurs
        etUsername.setError(null);
        etPassword.setError(null);
        
        // Récupérer les valeurs
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Valider les champs
        boolean cancel = false;
        View focusView = null;
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Le mot de passe est requis");
            focusView = etPassword;
            cancel = true;
        }
        
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("L'identifiant est requis");
            focusView = etUsername;
            cancel = true;
        }
        
        if (cancel) {
            focusView.requestFocus();
            return;
        }
        
        // Afficher l'indicateur de chargement
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        
        // Créer la requête de connexion
        LoginRequest loginRequest = new LoginRequest(username, password);
        
        // Appeler l'API pour authentifier
        ApiClient.getInstance(this).getApiService().loginGuardian(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    User user = loginResponse.getUser();
                    
                    if (user != null && user.isGuardian()) {
                        // Sauvegarder les informations de l'utilisateur
                        saveUserInfo(loginResponse);
                        
                        // Rediriger vers la liste des incidents
                        Intent intent = new Intent(GuardianLoginActivity.this, IncidentListActivity.class);
                        intent.putExtra("is_guardian", true);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(GuardianLoginActivity.this, "Cet utilisateur n'est pas un gardien", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Gérer l'erreur d'authentification
                    if (response.code() == 401) {
                        Toast.makeText(GuardianLoginActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GuardianLoginActivity.this, "Erreur d'authentification", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(GuardianLoginActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Sauvegarde les informations de l'utilisateur
     */
    private void saveUserInfo(LoginResponse loginResponse) {
        SharedPreferences prefs = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE);
        prefs.edit()
            .putString("auth_token", loginResponse.getAuthToken())
            .putInt("user_id", loginResponse.getUser().getId())
            .putString("user_role", loginResponse.getUser().getRole())
            .apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 