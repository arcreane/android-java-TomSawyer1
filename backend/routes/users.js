const express = require('express');
const router = express.Router();
const { db } = require('../database');

/**
 * Récupère tous les utilisateurs
 * GET /api/users
 */
router.get('/', (req, res) => {
  db.all('SELECT id, username, fullname, email, phone, role, created_at FROM users ORDER BY fullname', [], (err, rows) => {
    if (err) {
      console.error('Erreur lors de la récupération des utilisateurs:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    res.json(rows);
  });
});

/**
 * Récupère un utilisateur par son ID
 * GET /api/users/:id
 */
router.get('/:id', (req, res) => {
  const userId = req.params.id;
  
  db.get('SELECT id, username, fullname, email, phone, role, created_at FROM users WHERE id = ?', [userId], (err, user) => {
    if (err) {
      console.error('Erreur lors de la récupération de l\'utilisateur:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!user) {
      return res.status(404).json({ error: 'Utilisateur non trouvé' });
    }
    
    // Récupérer les appartements associés à l'utilisateur (si c'est un locataire ou propriétaire)
    if (user.role === 'tenant' || user.role === 'owner') {
      const query = `
        SELECT a.*, b.name as building_name, ua.relation_type
        FROM user_apartments ua
        JOIN apartments a ON ua.apartment_id = a.id
        JOIN buildings b ON a.building_id = b.id
        WHERE ua.user_id = ?
      `;
      
      db.all(query, [userId], (err, apartments) => {
        if (err) {
          console.error('Erreur lors de la récupération des appartements:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        user.apartments = apartments;
        
        // Récupérer les incidents signalés par l'utilisateur
        db.all('SELECT * FROM incidents WHERE reported_by = ? ORDER BY created_at DESC', [userId], (err, incidents) => {
          if (err) {
            console.error('Erreur lors de la récupération des incidents:', err);
            return res.status(500).json({ error: 'Erreur serveur' });
          }
          
          user.reported_incidents = incidents;
          
          res.json(user);
        });
      });
    } else if (user.role === 'guardian') {
      // Pour les gardiens, récupérer les incidents qui leur sont assignés
      db.all('SELECT * FROM incidents WHERE assigned_to = ? ORDER BY created_at DESC', [userId], (err, incidents) => {
        if (err) {
          console.error('Erreur lors de la récupération des incidents:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        user.assigned_incidents = incidents;
        
        res.json(user);
      });
    } else {
      res.json(user);
    }
  });
});

/**
 * Authentifie un utilisateur
 * POST /api/users/login
 */
router.post('/login', (req, res) => {
  const { username, password } = req.body;
  
  if (!username || !password) {
    return res.status(400).json({ error: 'Identifiants incomplets' });
  }
  
  db.get('SELECT id, username, fullname, email, phone, role FROM users WHERE username = ? AND password = ?', 
    [username, password], 
    (err, user) => {
      if (err) {
        console.error('Erreur lors de l\'authentification:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (!user) {
        return res.status(401).json({ error: 'Identifiants incorrects' });
      }
      
      res.json({
        ...user,
        auth_token: `token_${user.id}_${Date.now()}` // Simple simulation de token
      });
    }
  );
});

/**
 * Crée un nouvel utilisateur
 * POST /api/users
 */
router.post('/', (req, res) => {
  const { username, password, fullname, email, phone, role } = req.body;
  
  if (!username || !password || !fullname || !role) {
    return res.status(400).json({ error: 'Champs obligatoires manquants' });
  }
  
  // Vérifier si le nom d'utilisateur existe déjà
  db.get('SELECT id FROM users WHERE username = ?', [username], (err, existingUser) => {
    if (err) {
      console.error('Erreur lors de la vérification du nom d\'utilisateur:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (existingUser) {
      return res.status(409).json({ error: 'Ce nom d\'utilisateur existe déjà' });
    }
    
    // Vérifier si l'email existe déjà (s'il est fourni)
    let emailCheckCallback = (err, existingEmail) => {
      if (err) {
        console.error('Erreur lors de la vérification de l\'email:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (existingEmail) {
        return res.status(409).json({ error: 'Cet email est déjà utilisé' });
      }
      
      // Créer le nouvel utilisateur
      db.run(
        'INSERT INTO users (username, password, fullname, email, phone, role) VALUES (?, ?, ?, ?, ?, ?)',
        [username, password, fullname, email || null, phone || null, role],
        function(err) {
          if (err) {
            console.error('Erreur lors de la création de l\'utilisateur:', err);
            return res.status(500).json({ error: 'Erreur serveur' });
          }
          
          // Récupérer l'utilisateur créé
          db.get('SELECT id, username, fullname, email, phone, role, created_at FROM users WHERE id = ?', 
            [this.lastID], 
            (err, newUser) => {
              if (err) {
                console.error('Erreur lors de la récupération de l\'utilisateur créé:', err);
                return res.status(500).json({ error: 'Erreur serveur' });
              }
              
              res.status(201).json(newUser);
            }
          );
        }
      );
    };
    
    if (email) {
      db.get('SELECT id FROM users WHERE email = ?', [email], emailCheckCallback);
    } else {
      emailCheckCallback(null, null);
    }
  });
});

/**
 * Associe un utilisateur à un appartement
 * POST /api/users/:id/apartments
 */
router.post('/:id/apartments', (req, res) => {
  const userId = req.params.id;
  const { apartment_id, relation_type } = req.body;
  
  if (!apartment_id || !relation_type) {
    return res.status(400).json({ error: 'Champs obligatoires manquants' });
  }
  
  if (relation_type !== 'tenant' && relation_type !== 'owner') {
    return res.status(400).json({ error: 'Type de relation invalide' });
  }
  
  // Vérifier si l'utilisateur existe et est un locataire ou propriétaire
  db.get('SELECT id, role FROM users WHERE id = ?', [userId], (err, user) => {
    if (err) {
      console.error('Erreur lors de la vérification de l\'utilisateur:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!user) {
      return res.status(404).json({ error: 'Utilisateur non trouvé' });
    }
    
    if (user.role !== 'tenant' && user.role !== 'owner') {
      return res.status(400).json({ error: 'Seuls les locataires et propriétaires peuvent être associés à des appartements' });
    }
    
    // Vérifier si l'appartement existe
    db.get('SELECT id FROM apartments WHERE id = ?', [apartment_id], (err, apartment) => {
      if (err) {
        console.error('Erreur lors de la vérification de l\'appartement:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (!apartment) {
        return res.status(404).json({ error: 'Appartement non trouvé' });
      }
      
      // Vérifier si l'association existe déjà
      db.get(
        'SELECT * FROM user_apartments WHERE user_id = ? AND apartment_id = ?', 
        [userId, apartment_id], 
        (err, existingAssoc) => {
          if (err) {
            console.error('Erreur lors de la vérification de l\'association:', err);
            return res.status(500).json({ error: 'Erreur serveur' });
          }
          
          if (existingAssoc) {
            return res.status(409).json({ error: 'Cet utilisateur est déjà associé à cet appartement' });
          }
          
          // Créer l'association
          db.run(
            'INSERT INTO user_apartments (user_id, apartment_id, relation_type) VALUES (?, ?, ?)',
            [userId, apartment_id, relation_type],
            function(err) {
              if (err) {
                console.error('Erreur lors de la création de l\'association:', err);
                return res.status(500).json({ error: 'Erreur serveur' });
              }
              
              res.status(201).json({
                user_id: userId,
                apartment_id: apartment_id,
                relation_type: relation_type
              });
            }
          );
        }
      );
    });
  });
});

/**
 * Met à jour un utilisateur
 * PUT /api/users/:id
 */
router.put('/:id', (req, res) => {
  const userId = req.params.id;
  const { fullname, email, phone, password } = req.body;
  
  // Vérifier si au moins un champ à modifier est fourni
  if (!fullname && !email && !phone && !password) {
    return res.status(400).json({ error: 'Aucune donnée à mettre à jour' });
  }
  
  // Construire la requête de mise à jour
  const updateFields = [];
  const values = [];
  
  if (fullname) {
    updateFields.push('fullname = ?');
    values.push(fullname);
  }
  
  if (email) {
    updateFields.push('email = ?');
    values.push(email);
  }
  
  if (phone !== undefined) {
    updateFields.push('phone = ?');
    values.push(phone);
  }
  
  if (password) {
    updateFields.push('password = ?');
    values.push(password);
  }
  
  // Ajouter l'ID de l'utilisateur aux valeurs
  values.push(userId);
  
  const query = `
    UPDATE users
    SET ${updateFields.join(', ')}
    WHERE id = ?
  `;
  
  db.run(query, values, function(err) {
    if (err) {
      console.error('Erreur lors de la mise à jour de l\'utilisateur:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (this.changes === 0) {
      return res.status(404).json({ error: 'Utilisateur non trouvé ou aucune modification effectuée' });
    }
    
    // Récupérer l'utilisateur mis à jour
    db.get('SELECT id, username, fullname, email, phone, role, created_at FROM users WHERE id = ?', 
      [userId], 
      (err, updatedUser) => {
        if (err) {
          console.error('Erreur lors de la récupération de l\'utilisateur mis à jour:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        res.json(updatedUser);
      }
    );
  });
});

/**
 * Supprime un utilisateur
 * DELETE /api/users/:id
 */
router.delete('/:id', (req, res) => {
  const userId = req.params.id;
  
  // Vérifier si l'utilisateur existe
  db.get('SELECT * FROM users WHERE id = ?', [userId], (err, user) => {
    if (err) {
      console.error('Erreur lors de la vérification de l\'utilisateur:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!user) {
      return res.status(404).json({ error: 'Utilisateur non trouvé' });
    }
    
    // Vérifier s'il y a des incidents liés à cet utilisateur
    db.get('SELECT COUNT(*) as count FROM incidents WHERE reported_by = ? OR assigned_to = ?', [userId, userId], (err, result) => {
      if (err) {
        console.error('Erreur lors de la vérification des incidents:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (result.count > 0) {
        return res.status(409).json({ 
          error: 'Impossible de supprimer cet utilisateur car des incidents y sont associés' 
        });
      }
      
      // Supprimer les associations appartement-utilisateur
      db.run('DELETE FROM user_apartments WHERE user_id = ?', [userId], (err) => {
        if (err) {
          console.error('Erreur lors de la suppression des associations:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        // Supprimer l'utilisateur
        db.run('DELETE FROM users WHERE id = ?', [userId], function(err) {
          if (err) {
            console.error('Erreur lors de la suppression de l\'utilisateur:', err);
            return res.status(500).json({ error: 'Erreur serveur' });
          }
          
          if (this.changes === 0) {
            return res.status(404).json({ error: 'Utilisateur non trouvé' });
          }
          
          res.status(200).json({ message: 'Utilisateur supprimé avec succès' });
        });
      });
    });
  });
});

module.exports = router; 