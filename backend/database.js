const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Chemin de la base de données
const dbPath = path.resolve(__dirname, 'guardconnect.db');

// Connexion à la base de données
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Erreur de connexion à la base de données:', err.message);
  } else {
    console.log('Connexion à la base de données SQLite réussie');
  }
});

// Initialisation de la base de données avec les tables
const initDb = () => {
  return new Promise((resolve, reject) => {
    // Création de la table des utilisateurs (gardiens, locataires, propriétaires)
    db.run(`CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL,
      fullname TEXT NOT NULL,
      email TEXT UNIQUE,
      phone TEXT,
      role TEXT NOT NULL, -- 'guardian', 'tenant', 'owner'
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )`, (err) => {
      if (err) {
        console.error('Erreur création table users:', err.message);
        reject(err);
        return;
      }

      // Création de la table des bâtiments
      db.run(`CREATE TABLE IF NOT EXISTS buildings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        address TEXT NOT NULL,
        code TEXT NOT NULL, -- code pour les gardiens
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )`, (err) => {
        if (err) {
          console.error('Erreur création table buildings:', err.message);
          reject(err);
          return;
        }

        // Création de la table des appartements
        db.run(`CREATE TABLE IF NOT EXISTS apartments (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          building_id INTEGER NOT NULL,
          number TEXT NOT NULL,
          floor INTEGER,
          FOREIGN KEY (building_id) REFERENCES buildings (id)
        )`, (err) => {
          if (err) {
            console.error('Erreur création table apartments:', err.message);
            reject(err);
            return;
          }

          // Création de la table des incidents
          db.run(`CREATE TABLE IF NOT EXISTS incidents (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            status TEXT NOT NULL, -- 'pending', 'in_progress', 'resolved'
            priority TEXT NOT NULL, -- 'low', 'medium', 'high'
            location TEXT,
            tracking_code TEXT NOT NULL, -- Code de suivi unique pour les utilisateurs
            apartment_id INTEGER,
            building_id INTEGER NOT NULL,
            reported_by INTEGER NOT NULL,
            assigned_to INTEGER,
            image_path TEXT,
            email TEXT, -- Adresse email pour le suivi
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (apartment_id) REFERENCES apartments (id),
            FOREIGN KEY (building_id) REFERENCES buildings (id),
            FOREIGN KEY (reported_by) REFERENCES users (id),
            FOREIGN KEY (assigned_to) REFERENCES users (id)
          )`, (err) => {
            if (err) {
              console.error('Erreur création table incidents:', err.message);
              reject(err);
              return;
            }

            // Création de la table des commentaires sur les incidents
            db.run(`CREATE TABLE IF NOT EXISTS comments (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              incident_id INTEGER NOT NULL,
              user_id INTEGER NOT NULL,
              comment TEXT NOT NULL,
              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
              FOREIGN KEY (incident_id) REFERENCES incidents (id),
              FOREIGN KEY (user_id) REFERENCES users (id)
            )`, (err) => {
              if (err) {
                console.error('Erreur création table comments:', err.message);
                reject(err);
                return;
              }

              // Création de la table d'association entre utilisateurs et appartements
              db.run(`CREATE TABLE IF NOT EXISTS user_apartments (
                user_id INTEGER NOT NULL,
                apartment_id INTEGER NOT NULL,
                relation_type TEXT NOT NULL, -- 'tenant', 'owner'
                PRIMARY KEY (user_id, apartment_id),
                FOREIGN KEY (user_id) REFERENCES users (id),
                FOREIGN KEY (apartment_id) REFERENCES apartments (id)
              )`, (err) => {
                if (err) {
                  console.error('Erreur création table user_apartments:', err.message);
                  reject(err);
                  return;
                }

                // Insertion de données initiales si nécessaire
                insertSampleData().then(() => {
                  resolve();
                }).catch(err => {
                  console.error('Erreur lors de l\'insertion des données d\'exemple:', err);
                  reject(err);
                });
              });
            });
          });
        });
      });
    });
  });
};

// Fonction pour insérer des données d'exemple
const insertSampleData = () => {
  return new Promise((resolve, reject) => {
    // Vérifier s'il y a déjà des données dans la table des bâtiments
    db.get('SELECT COUNT(*) as count FROM buildings', [], (err, row) => {
      if (err) {
        reject(err);
        return;
      }

      // Si aucun bâtiment n'existe, insérer des données d'exemple
      if (row.count === 0) {
        // Insérer un bâtiment
        db.run('INSERT INTO buildings (name, address, code) VALUES (?, ?, ?)',
          ['Résidence Les Lilas', '123 Avenue des Fleurs, 75001 Paris', '1234'],
          function(err) {
            if (err) {
              reject(err);
              return;
            }
            const buildingId = this.lastID;

            // Insérer quelques appartements
            const aptStmt = db.prepare('INSERT INTO apartments (building_id, number, floor) VALUES (?, ?, ?)');
            for (let i = 1; i <= 5; i++) {
              aptStmt.run(buildingId, `${i}01`, i, (err) => {
                if (err) {
                  console.error('Erreur insertion appartement:', err);
                }
              });
            }
            aptStmt.finalize();

            // Insérer un utilisateur gardien
            db.run('INSERT INTO users (username, password, fullname, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)',
              ['gardien1', 'password123', 'Jean Dupont', 'gardien@example.com', '0123456789', 'guardian'],
              (err) => {
                if (err) {
                  reject(err);
                  return;
                }
                
                // Insérer un utilisateur locataire
                db.run('INSERT INTO users (username, password, fullname, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)',
                  ['locataire1', 'password123', 'Marie Martin', 'locataire@example.com', '0123456789', 'tenant'],
                  (err) => {
                    if (err) {
                      reject(err);
                      return;
                    }
                    
                    resolve();
                  });
              });
          });
      } else {
        resolve();
      }
    });
  });
};

module.exports = {
  db,
  initDb
}; 