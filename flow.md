# Flux de Navigation - FGuardConnect

## 1. Écran de Démarrage
- **Fichier Java**: `LoginActivity.java`
- **Layout**: `activity_login.xml`
- **Actions**:
  - Connexion utilisateur
  - Navigation vers `GuardianLoginActivity` pour les gardiens

## 2. Écran Principal
- **Fichier Java**: `MainActivity.java`
- **Layout**: `activity_main.xml`
- **Actions**:
  - Affichage du tableau de bord
  - Navigation vers la liste des incidents
  - Accès au formulaire de signalement

## 3. Liste des Incidents
- **Fichier Java**: `IncidentListActivity.java`
- **Layout**: `activity_incident_list.xml`
- **Actions**:
  - Affichage de la liste des incidents
  - Navigation vers les détails d'un incident
  - Filtrage des incidents

## 4. Détails d'un Incident
- **Fichier Java**: `IncidentDetailActivity.java`
- **Layout**: `activity_incident_detail.xml`
- **Actions**:
  - Affichage des détails complets
  - Mise à jour du statut
  - Ajout de commentaires

## 5. Formulaire de Signalement
- **Fichier Java**: `FormActivity.java`
- **Layout**: `activity_form.xml`
- **Actions**:
  - Sélection du bâtiment
  - Sélection de l'appartement
  - Capture de photo (caméra/galerie)
  - Remplissage des informations
  - Soumission du formulaire

## 6. Suivi d'Incident
- **Fichier Java**: `FollowActivity.java`
- **Layout**: `activity_follow.xml`
- **Actions**:
  - Recherche par code de suivi
  - Affichage du statut
  - Historique des mises à jour

## Flux de Navigation Typique

1. **Démarrage** → `LoginActivity`
   - L'utilisateur se connecte
   - Vérification des identifiants

2. **Accueil** → `MainActivity`
   - Affichage du menu principal
   - Options de navigation

3. **Signalement** → `FormActivity`
   - Remplissage du formulaire
   - Capture de photo
   - Génération du code de suivi

4. **Suivi** → `FollowActivity`
   - Utilisation du code de suivi
   - Consultation des mises à jour

## Gestion des Ressources

### Layouts XML
- `activity_login.xml` - Écran de connexion
- `activity_main.xml` - Écran principal
- `activity_incident_list.xml` - Liste des incidents
- `activity_incident_detail.xml` - Détails d'un incident
- `activity_form.xml` - Formulaire de signalement
- `activity_follow.xml` - Suivi d'incident

### Ressources
- `res/values/strings.xml` - Textes de l'application
- `res/values/colors.xml` - Couleurs
- `res/values/styles.xml` - Styles
- `res/drawable/` - Images et icônes

## Gestion des États
- Utilisation de `SharedPreferences` pour la persistance
- Gestion des sessions utilisateur
- Stockage des codes de suivi
- Cache des images

## Navigation entre les Écrans
- Utilisation d'`Intent` pour la navigation
- Passage de données entre les activités
- Gestion du retour arrière
- Animations de transition 