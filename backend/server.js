const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');
const database = require('./database');
const incidentRoutes = require('./routes/incidents');
const buildingRoutes = require('./routes/buildings');
const userRoutes = require('./routes/users');
const uploadRoutes = require('./routes/uploads');
const apartmentRoutes = require('./routes/apartments');

const app = express();
const PORT = process.env.PORT || 3000;

// Middlewares
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Middleware pour logger toutes les requêtes
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} - IP: ${req.ip}`);
  next();
});

// Route de santé pour vérifier que l'API est en ligne
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'API is running' });
});

// Route de base pour l'API
app.get('/api', (req, res) => {
  res.json({ message: 'Bienvenue sur l\'API GuardConnect' });
});

// Servir les fichiers statiques du dossier uploads
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Routes pour l'authentification des gardiens
app.post('/api/auth/guardian', (req, res) => {
  const { username, password } = req.body;
  
  if (!username || !password) {
    return res.status(400).json({ error: 'Identifiants incomplets' });
  }
  
  // Vérifier les identifiants et s'assurer que l'utilisateur est un gardien
  database.db.get(
    'SELECT id, username, fullname, email, phone, role FROM users WHERE username = ? AND password = ? AND role = "guardian"', 
    [username, password], 
    (err, user) => {
      if (err) {
        console.error('Erreur lors de l\'authentification du gardien:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (!user) {
        return res.status(401).json({ error: 'Identifiants incorrects ou utilisateur non autorisé' });
      }
      
      // Générer un token simple
      const authToken = `token_${user.id}_${Date.now()}`;
      
      res.json({
        user: user,
        auth_token: authToken
      });
    }
  );
});

// Routes
app.use('/api/incidents', incidentRoutes);
app.use('/api/buildings', buildingRoutes);
app.use('/api/users', userRoutes);
app.use('/api/uploads', uploadRoutes);
app.use('/api/apartments', apartmentRoutes);

// Base route
app.get('/', (req, res) => {
  res.json({ message: 'Bienvenue sur l\'API GuardConnect' });
});

// Initialisation de la base de données
database.initDb().then(() => {
  console.log('Base de données initialisée');
  
  // Démarrage du serveur sur toutes les interfaces réseau
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Serveur démarré sur le port ${PORT}`);
    console.log(`Accès réseau local à http://localhost:${PORT}`);
    console.log(`Accès pour Android à http://192.168.1.58:${PORT}`);
    console.log(`Accès pour l'émulateur à http://10.0.2.2:${PORT}`);
  });
}).catch(err => {
  console.error('Erreur lors de l\'initialisation de la base de données:', err);
}); 