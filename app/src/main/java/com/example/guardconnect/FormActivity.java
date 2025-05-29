package com.example.guardconnect;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.guardconnect.api.ApiClient;
import com.example.guardconnect.api.models.Apartment;
import com.example.guardconnect.api.models.Building;
import com.example.guardconnect.api.models.Incident;
import com.example.guardconnect.api.models.ImageUploadResponse;
import com.example.guardconnect.api.models.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FormActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;
    private static final int STORAGE_PERMISSION_CODE = 1002;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private ImageView imageView;
    private Button cameraButton;
    private Button submitButton;
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText floorEditText;
    private EditText doorNumberEditText;
    private EditText descriptionEditText;
    private Spinner buildingSpinner;
    private Spinner apartmentSpinner;
    private String selectedImagePath;
    
    private List<Building> buildings = new ArrayList<>();
    private List<Apartment> apartments = new ArrayList<>();
    private Building selectedBuilding = null;
    private Apartment selectedApartment = null;
    
    private Bitmap capturedImage = null;
    private String imagePath = null;
    private ApiClient apiClient;
    private ProgressBar progressBar;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        
        // Retirer complètement la barre de titre
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_form);
        
        // Activer le bouton retour dans la barre d'action
        Utils.setupActionBar(this, "Déclarer un incident");
        
        // Initialiser ApiClient
        apiClient = ApiClient.getInstance(this);
        
        // Vérifier la connexion au backend
        checkBackendConnection();
        
        // Initialiser les vues
        imageView = findViewById(R.id.capturedImage);
        cameraButton = findViewById(R.id.openCamera);
        submitButton = findViewById(R.id.submitButton);
        nameEditText = findViewById(R.id.names);
        emailEditText = findViewById(R.id.mails);
        floorEditText = findViewById(R.id.steps);
        doorNumberEditText = findViewById(R.id.portes);
        descriptionEditText = findViewById(R.id.descriptions);
        progressBar = findViewById(R.id.progressBar);
        buildingSpinner = findViewById(R.id.building_spinner);
        apartmentSpinner = findViewById(R.id.apartment_spinner);

        // Configurer le bouton caméra pour pouvoir aussi ouvrir la galerie (appui long)
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Afficher un dialogue pour choisir entre la caméra et la galerie
                AlertDialog.Builder builder = new AlertDialog.Builder(FormActivity.this);
                builder.setTitle("Choisir une photo");
                builder.setItems(new CharSequence[]{"Prendre une photo", "Choisir depuis la galerie"}, 
                        (dialog, which) -> {
                            if (which == 0) {
                                // Option caméra
                                if (ContextCompat.checkSelfPermission(
                                        FormActivity.this, 
                                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    openCamera();
                                } else {
                                    ActivityCompat.requestPermissions(
                                            FormActivity.this,
                                            new String[]{Manifest.permission.CAMERA},
                                            CAMERA_PERMISSION_CODE);
                                }
                            } else {
                                // Option galerie
                                openGallery();
                            }
                        });
                builder.show();
            }
        });
        
        // Configurer le bouton d'envoi
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitIncident();
            }
        });
        
        // Charger les bâtiments
        loadBuildings();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.form), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    /**
     * Charge la liste des bâtiments depuis l'API
     */
    private void loadBuildings() {
        progressBar.setVisibility(View.VISIBLE);
        
        apiClient.getApiService().getBuildings().enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    buildings = response.body();
                    
                    if (buildings.isEmpty()) {
                        Toast.makeText(FormActivity.this, 
                                "Aucun bâtiment disponible. Veuillez contacter l'administrateur.", 
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Préparer les données pour le spinner
                    List<String> buildingNames = new ArrayList<>();
                    for (Building building : buildings) {
                        buildingNames.add(building.getName());
                    }
                    
                    // Configurer l'adaptateur
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            FormActivity.this, 
                            android.R.layout.simple_spinner_item, 
                            buildingNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    buildingSpinner.setAdapter(adapter);
                    
                    // Configurer l'écouteur
                    buildingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedBuilding = buildings.get(position);
                            
                            // Effacer les appartements précédents et le choix d'appartement
                            apartments.clear();
                            selectedApartment = null;
                            
                            // Charger les appartements du bâtiment sélectionné
                            loadApartments(selectedBuilding.getId());
                        }
                        
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            selectedBuilding = null;
                        }
                    });
                } else {
                    Toast.makeText(FormActivity.this, 
                            "Erreur lors du chargement des bâtiments: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Building>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FormActivity.this, 
                        "Impossible de se connecter au serveur", 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Charge la liste des appartements pour un bâtiment donné
     */
    private void loadApartments(int buildingId) {
        progressBar.setVisibility(View.VISIBLE);
        
        apiClient.getBuildingApartments(buildingId, new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Apartment> apartments = response.body();
                    
                    // Mettre à jour le bâtiment sélectionné avec les appartements récupérés
                    if (selectedBuilding != null) {
                        selectedBuilding.setApartments(apartments);
                    }
                    
                    // Afficher les appartements
                    displayApartments(apartments);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FormActivity.this, "Erreur lors du chargement des appartements", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Apartment>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FormActivity.this, "Impossible de charger les appartements", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Affiche la liste des appartements dans le spinner
     */
    private void displayApartments(List<Apartment> apartmentList) {
        progressBar.setVisibility(View.GONE);
        
        apartments = apartmentList;
        
        if (apartments == null || apartments.isEmpty()) {
            Toast.makeText(FormActivity.this, "Aucun appartement disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Préparer les données pour le spinner
        List<String> apartmentNames = new ArrayList<>();
        for (Apartment apartment : apartments) {
            // N'afficher que le numéro d'appartement sans préfixe
            String displayText = apartment.getNumber();
            // Supprimer tout préfixe comme "Apt" et garder seulement les chiffres
            if (displayText != null) {
                displayText = displayText.replaceAll("Apt\\s*", "");
                // Supprimer toutes les lettres pour ne garder que les chiffres
                displayText = displayText.replaceAll("[^0-9]", "");
            }
            
            apartmentNames.add(displayText);
        }
        
        // Vérifier que la liste n'est pas vide après traitement
        if (apartmentNames.isEmpty()) {
            return;
        }
        
        // Configurer l'adaptateur
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                FormActivity.this, 
                android.R.layout.simple_spinner_item, 
                apartmentNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        apartmentSpinner.setAdapter(adapter);
        
        // Configurer l'écouteur
        apartmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < apartments.size()) {
                    selectedApartment = apartments.get(position);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedApartment = null;
            }
        });
    }

    /**
     * Ouvre la galerie pour sélectionner une image
     */
    private void openGallery() {
        // Vérifier la permission d'accès au stockage pour Android < 10
        if (ContextCompat.checkSelfPermission(
                FormActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(
                    FormActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }
    
    /**
     * Méthode simplifiée pour créer un incident
     */
    private boolean validateForm() {
        // Vérifier les champs obligatoires
        if (nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError("Le titre est obligatoire");
            return false;
        }
        
        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            descriptionEditText.setError("La description est obligatoire");
            return false;
        }
        
        if (selectedBuilding == null) {
            Toast.makeText(this, "Veuillez sélectionner un bâtiment", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Valider l'email si renseigné
        String email = emailEditText.getText().toString().trim();
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Format d'email invalide");
            return false;
        }
        
        return true;
    }

    private void submitIncident() {
        try {
            if (!validateForm()) {
                return;
            }
            
            // Récupérer les valeurs des champs
            String title = nameEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String floorStr = floorEditText.getText().toString().trim();
            String doorNumber = doorNumberEditText.getText().toString().trim();
            String location = floorStr + " " + doorNumber;
            String email = emailEditText.getText().toString().trim();
            
            // Afficher l'indicateur de chargement
            progressBar.setVisibility(View.VISIBLE);
            
            // Déterminer l'ID utilisateur pour le signalement
            int userId;
            if (apiClient.getCurrentUser() == null) {
                // Utiliser un ID par défaut pour le test (user gardien)
                userId = 1;
            } else {
                // Utiliser l'ID de l'utilisateur connecté
                userId = apiClient.getCurrentUser().getId();
            }
            
            // Si nous avons une image, la télécharger d'abord
            if (capturedImage != null) {
                uploadImageThenCreateIncident(capturedImage, title, description, location, email, userId);
            } else {
                // Pas d'image, créer l'incident directement
                createIncident(title, description, location, email, userId, null);
            }
            
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Télécharge l'image sur le serveur puis crée l'incident
     */
    private void uploadImageThenCreateIncident(Bitmap image, String title, String description, 
                                              String location, String email, int userId) {
        try {
            // Convertir le bitmap en fichier temporaire
            File cacheDir = getCacheDir();
            
            // Créer un nom de fichier qui contient des informations d'identification
            String userIdentifier = !email.isEmpty() ? email.split("@")[0] : "user_" + userId;
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(title.length(), 10));
            String fileName = "incident_" + userIdentifier + "_" + sanitizedTitle + "_" + System.currentTimeMillis() + ".jpg";
            
            File tempImageFile = File.createTempFile("temp_" + fileName, ".jpg", cacheDir);
            
            FileOutputStream fos = new FileOutputStream(tempImageFile);
            image.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();
            
            // Créer la requête multipart
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempImageFile);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", fileName, requestFile);
            
            // Envoyer l'image au serveur
            apiClient.getApiService().uploadIncidentImage(imagePart).enqueue(new Callback<ImageUploadResponse>() {
                @Override
                public void onResponse(Call<ImageUploadResponse> call, Response<ImageUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imagePath = response.body().getImagePath();
                        // Maintenant créer l'incident avec le chemin de l'image
                        createIncident(title, description, location, email, userId, imagePath);
                    } else {
                        // Continuer sans image
                        createIncident(title, description, location, email, userId, null);
                    }
                    
                    // Supprimer le fichier temporaire
                    tempImageFile.delete();
                }
                
                @Override
                public void onFailure(Call<ImageUploadResponse> call, Throwable t) {
                    // Continuer sans image
                    createIncident(title, description, location, email, userId, null);
                    
                    // Supprimer le fichier temporaire
                    tempImageFile.delete();
                }
            });
        } catch (Exception e) {
            // Continuer sans image
            createIncident(title, description, location, email, userId, null);
        }
    }
    
    /**
     * Crée un incident avec ou sans image
     */
    private void createIncident(String title, String description, String location, 
                               String email, int userId, String imagePath) {
        // Créer l'objet incident pour l'API
        Map<String, Object> incidentMap = new HashMap<>();
        incidentMap.put("title", title);
        incidentMap.put("description", description);
        incidentMap.put("priority", "medium"); // Priorité par défaut
        incidentMap.put("location", location);
        incidentMap.put("building_id", selectedBuilding.getId());
        incidentMap.put("reported_by", userId);
        
        // Stocker l'ID de l'utilisateur dans les préférences partagées
        SharedPreferences prefs = getSharedPreferences("GuardConnectPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("user_id", userId);
        editor.apply();
        
        // Ajouter l'email s'il est renseigné
        if (!email.isEmpty()) {
            incidentMap.put("email", email);
        }
        
        // Ajouter l'ID de l'appartement si sélectionné
        if (selectedApartment != null) {
            incidentMap.put("apartment_id", selectedApartment.getId());
        }
        
        // Ajouter le chemin de l'image si disponible
        if (imagePath != null && !imagePath.isEmpty()) {
            incidentMap.put("image_path", imagePath);
        }
        
        // Appeler l'API pour créer l'incident
        apiClient.getApiService().createIncident(incidentMap).enqueue(new Callback<Incident>() {
            @Override
            public void onResponse(Call<Incident> call, Response<Incident> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Incident createdIncident = response.body();
                    
                    // Si l'incident a un code de suivi, le stocker et créer un dossier avec ce nom
                    String trackingCode = createdIncident.getTrackingCode();
                    if (imagePath != null && capturedImage != null && trackingCode != null) {
                        // Enregistrer l'image localement dans un dossier nommé avec le code de suivi
                        saveImageLocally(capturedImage, trackingCode);
                    }
                    
                    // Afficher le code de suivi dans une boîte de dialogue
                    showTrackingCodeDialog(trackingCode);
                    
                    Toast.makeText(FormActivity.this, "Incident signalé avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FormActivity.this, "Erreur lors de la création de l'incident", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Incident> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FormActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Enregistre l'image localement dans un dossier nommé avec le code de suivi
     */
    private String saveImageLocally(Bitmap image, String trackingCode) {
        try {
            // Créer le dossier de stockage interne de l'application s'il n'existe pas
            File uploadDir = new File(getFilesDir(), "uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Créer un sous-dossier avec le code de suivi comme nom
            File incidentDir = new File(uploadDir, trackingCode);
            if (!incidentDir.exists()) {
                incidentDir.mkdirs();
            }
            
            // Récupérer les informations de l'utilisateur pour le nom du fichier
            String email = emailEditText.getText().toString().trim();
            String title = nameEditText.getText().toString().trim();
            int userId = 0;
            if (apiClient.getCurrentUser() != null) {
                userId = apiClient.getCurrentUser().getId();
            }
            
            // Créer un nom de fichier qui identifie clairement le propriétaire
            String userIdentifier = !email.isEmpty() ? email.split("@")[0] : "user_" + userId;
            String sanitizedTitle = title.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(title.length(), 10));
            String imageFileName = "image_" + userIdentifier + "_" + sanitizedTitle + "_" + System.currentTimeMillis() + ".jpg";
            
            // Enregistrer l'image dans ce dossier
            File imageFile = new File(incidentDir, imageFileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // Créer un fichier texte avec les métadonnées pour faciliter l'identification
            File metadataFile = new File(incidentDir, "metadata.txt");
            boolean isNewFile = !metadataFile.exists();
            
            FileOutputStream metaFos = new FileOutputStream(metadataFile, true); // append mode
            String metadataContent = "Image: " + imageFileName + "\n" +
                                    "Date: " + new java.util.Date().toString() + "\n" +
                                    "Titre: " + title + "\n" +
                                    "Email: " + (email.isEmpty() ? "Non fourni" : email) + "\n" +
                                    "UserId: " + userId + "\n" +
                                    "Tracking Code: " + trackingCode + "\n" +
                                    "----------------------------------------\n";
            metaFos.write(metadataContent.getBytes());
            metaFos.close();
            
            return imageFile.getAbsolutePath();
            
        } catch (Exception e) {
            // Ignorer l'erreur, ce n'est pas critique
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission de caméra refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission d'accès au stockage refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == CAMERA_REQUEST_CODE && data.getExtras() != null) {
                    // Image provenant de la caméra
                    capturedImage = (Bitmap) data.getExtras().get("data");
                    processCapturedImage();
                } else if (requestCode == GALLERY_REQUEST_CODE && data.getData() != null) {
                    // Image provenant de la galerie
                    Uri selectedImageUri = data.getData();
                    capturedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    processCapturedImage();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Erreur avec l'image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Continuer sans image
                capturedImage = null;
            }
        }
    }
    
    /**
     * Traite l'image capturée (redimensionnement et affichage)
     */
    private void processCapturedImage() {
        if (capturedImage != null) {
            // Obtenir les dimensions de l'ImageView
            int targetWidth = imageView.getWidth();
            if (targetWidth <= 0) targetWidth = imageView.getLayoutParams().width;
            if (targetWidth <= 0) targetWidth = 800; // Valeur par défaut si tout échoue
            
            int targetHeight = imageView.getHeight();
            if (targetHeight <= 0) targetHeight = imageView.getLayoutParams().height;
            if (targetHeight <= 0) targetHeight = 600; // Valeur par défaut si tout échoue
            
            // Dimensions de l'image originale
            int width = capturedImage.getWidth();
            int height = capturedImage.getHeight();
            
            // Calculer les dimensions pour conserver le ratio tout en maximisant l'espace
            float ratio = Math.min((float) targetWidth / width, (float) targetHeight / height);
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);
            
            // Redimensionner l'image
            capturedImage = Bitmap.createScaledBitmap(capturedImage, newWidth, newHeight, true);
            
            // Afficher l'image avec un fond clair pour mieux voir ses contours
            imageView.setBackgroundColor(getResources().getColor(android.R.color.white));
            imageView.setImageBitmap(capturedImage);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Vérifie la connexion au serveur backend
     */
    private void checkBackendConnection() {
        if (submitButton != null) {
            submitButton.setEnabled(false);
        }
        
        Toast.makeText(this, "Vérification de la connexion au serveur...", Toast.LENGTH_SHORT).show();
        
        executorService.execute(() -> {
            try {
                String serverUrl = ApiClient.BASE_URL + "buildings";
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                
                int responseCode = connection.getResponseCode();
                
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(FormActivity.this, "Connexion réussie", Toast.LENGTH_LONG).show();
                        if (submitButton != null) {
                            submitButton.setEnabled(true);
                        }
                    } else {
                        Toast.makeText(FormActivity.this, "Erreur de connexion: Code " + responseCode, Toast.LENGTH_LONG).show();
                        if (submitButton != null) {
                            submitButton.setEnabled(true);
                            submitButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                        }
                    }
                });
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(FormActivity.this, "Erreur de connexion: " + errorMessage, Toast.LENGTH_LONG).show();
                    if (submitButton != null) {
                        submitButton.setEnabled(true);
                        submitButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    }
                });
            }
        });
        
        apiClient.checkConnection(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                runOnUiThread(() -> {
                    if (submitButton != null) {
                        submitButton.setEnabled(true);
                    }
                    Toast.makeText(FormActivity.this, "Connexion au serveur établie", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                runOnUiThread(() -> {
                    if (submitButton != null) {
                        submitButton.setEnabled(true);
                    }
                    Toast.makeText(FormActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Affiche une boîte de dialogue avec le code de suivi
     */
    private void showTrackingCodeDialog(String trackingCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Incident signalé avec succès !");
        builder.setMessage("Conservez ce code pour suivre l'avancement de votre incident (vous le recevrez par mail) :\n\n" + trackingCode);
        builder.setPositiveButton("Copier le code", (dialog, which) -> {
            // Copier le code dans le presse-papier
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Tracking Code", trackingCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copié dans le presse-papier", Toast.LENGTH_SHORT).show();
            finish(); // Retourner à l'écran précédent
        });
        builder.setNegativeButton("Fermer", (dialog, which) -> {
            finish(); // Retourner à l'écran précédent
        });
        builder.setCancelable(false); // Empêcher de fermer en cliquant à l'extérieur
        builder.show();
    }

    /**
     * Méthode temporaire pour créer des données de test d'appartements
     * Utilisée uniquement en cas d'échec de la récupération depuis l'API
     */
    private void addTestApartments() {
        // Créer une liste d'appartements de test basée sur la structure de la base de données
        List<Apartment> testApartments = new ArrayList<>();
        
        // Utiliser des numéros spécifiques pour différents bâtiments si possible
        int[] realApartmentNumbers;
        
        if (selectedBuilding != null && "Les Allées Vertes".equals(selectedBuilding.getName())) {
            // Numéros spécifiques pour Les Allées Vertes
            realApartmentNumbers = new int[]{112, 221, 331, 411, 511};
        } else {
            // Numéros génériques pour les autres bâtiments
            realApartmentNumbers = new int[]{101, 201, 301, 401, 501};
        }
        
        // Ajouter les appartements avec des données qui ressemblent à celles de la base de données
        for (int i = 0; i < realApartmentNumbers.length; i++) {
            Apartment apt = new Apartment();
            apt.setId(1000 + i); // IDs fictifs au-dessus de 1000 pour éviter les conflits
            apt.setBuildingId(selectedBuilding != null ? selectedBuilding.getId() : 1);
            // Convertir en String car la propriété number est de type String
            apt.setNumber(String.valueOf(realApartmentNumbers[i]));
            
            // Déterminer l'étage d'après le premier chiffre du numéro
            int floor = i + 1; // Un étage par appartement de test
            apt.setFloor(floor);
            
            testApartments.add(apt);
        }
        
        // Mettre à jour le bâtiment sélectionné avec les appartements de test
        if (selectedBuilding != null) {
            selectedBuilding.setApartments(testApartments);
        }
        
        // Afficher les appartements de test
        displayApartments(testApartments);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void handleImageResult(Uri imageUri) {
        try {
            String imagePath = getRealPathFromUri(imageUri);
            if (imagePath != null) {
                // Utiliser decodeSampledBitmapFromPath pour charger l'image avec une taille optimisée
                Bitmap bitmap = decodeSampledBitmapFromPath(imagePath, 1024, 1024);
                if (bitmap != null) {
                    // Générer un code de suivi temporaire pour le dossier
                    String trackingCode = "TEMP_" + System.currentTimeMillis();
                    selectedImagePath = saveImageLocally(bitmap, trackingCode);
                    if (selectedImagePath != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        
        if (cursor == null) {
            return uri.getPath();
        }
        
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        
        return path;
    }
}