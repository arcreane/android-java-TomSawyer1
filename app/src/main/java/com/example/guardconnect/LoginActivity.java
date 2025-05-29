package com.example.guardconnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.text.InputType;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {


    private Button gardbutton;
    private Button locbutton;
    private Button folbutton;
    private LoginActivity activity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Charger la langue préférée
        Utils.loadSavedLanguage(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Activer le bouton retour dans la barre d'action
        Utils.setupActionBar(this, "Connexion");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.activity = this;

this.gardbutton = (Button) findViewById(R.id.buttongard);
gardbutton.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

        AlertDialog.Builder myPopup = new AlertDialog.Builder(activity);
        myPopup.setTitle("Gardiens");

        myPopup.setMessage("Entrez le code du Batiment");

        //champ de mot de passe
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        myPopup.setView(input); // Ajoute l input dans l alertDialog

        myPopup.setPositiveButton("Valider", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Bon retour", Toast.LENGTH_SHORT).show();
            }
        });
        myPopup.show();
    }
});

        this.locbutton = (Button) findViewById(R.id.buttonloc);
        locbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent otherActivity = new Intent(getApplicationContext(), FormActivity.class);
                startActivity(otherActivity);
                finish();
            }
        });

        this.folbutton = (Button) findViewById(R.id.buttonfol);
        folbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent otherActivity = new Intent(getApplicationContext(), FollowActivity.class);
                startActivity(otherActivity);
                finish();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}