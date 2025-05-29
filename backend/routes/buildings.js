const express = require('express');
const router = express.Router();
const { db } = require('../database');

/**
 * Récupère tous les bâtiments
 * GET /api/buildings
 */
router.get('/', (req, res) => {
  db.all('SELECT * FROM buildings ORDER BY name', [], (err, rows) => {
    if (err) {
      console.error('Erreur lors de la récupération des bâtiments:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    res.json(rows);
  });
});

/**
 * Récupère un bâtiment par son ID
 * GET /api/buildings/:id
 */
router.get('/:id', (req, res) => {
  const buildingId = req.params.id;
  
  db.get('SELECT * FROM buildings WHERE id = ?', [buildingId], (err, building) => {
    if (err) {
      console.error('Erreur lors de la récupération du bâtiment:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!building) {
      return res.status(404).json({ error: 'Bâtiment non trouvé' });
    }
    
    // Récupérer les appartements de ce bâtiment
    db.all('SELECT * FROM apartments WHERE building_id = ? ORDER BY number', [buildingId], (err, apartments) => {
      if (err) {
        console.error('Erreur lors de la récupération des appartements:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      // Ajouter les appartements à l'objet bâtiment
      building.apartments = apartments;
      
      // Récupérer les incidents de ce bâtiment
      db.all(`
        SELECT i.*, u.fullname as reporter_name
        FROM incidents i
        LEFT JOIN users u ON i.reported_by = u.id
        WHERE i.building_id = ?
        ORDER BY i.created_at DESC
        LIMIT 10
      `, [buildingId], (err, incidents) => {
        if (err) {
          console.error('Erreur lors de la récupération des incidents:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        // Ajouter les incidents récents à l'objet bâtiment
        building.recent_incidents = incidents;
        
        res.json(building);
      });
    });
  });
});

/**
 * Vérifie le code d'un bâtiment (pour accès gardien)
 * POST /api/buildings/verify-code
 */
router.post('/verify-code', (req, res) => {
  const { code } = req.body;
  
  if (!code) {
    return res.status(400).json({ error: 'Code manquant' });
  }
  
  db.get('SELECT id, name FROM buildings WHERE code = ?', [code], (err, building) => {
    if (err) {
      console.error('Erreur lors de la vérification du code:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!building) {
      return res.status(401).json({ error: 'Code incorrect' });
    }
    
    res.json({
      verified: true,
      building_id: building.id,
      building_name: building.name
    });
  });
});

/**
 * Crée un nouveau bâtiment
 * POST /api/buildings
 */
router.post('/', (req, res) => {
  const { name, address, code } = req.body;
  
  if (!name || !address || !code) {
    return res.status(400).json({ error: 'Champs obligatoires manquants' });
  }
  
  db.run(
    'INSERT INTO buildings (name, address, code) VALUES (?, ?, ?)',
    [name, address, code],
    function(err) {
      if (err) {
        console.error('Erreur lors de la création du bâtiment:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      // Récupérer le bâtiment créé
      db.get('SELECT * FROM buildings WHERE id = ?', [this.lastID], (err, building) => {
        if (err) {
          console.error('Erreur lors de la récupération du bâtiment créé:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        res.status(201).json(building);
      });
    }
  );
});

/**
 * Ajoute un appartement à un bâtiment
 * POST /api/buildings/:id/apartments
 */
router.post('/:id/apartments', (req, res) => {
  const buildingId = req.params.id;
  const { number, floor } = req.body;
  
  if (!number) {
    return res.status(400).json({ error: 'Numéro d\'appartement manquant' });
  }
  
  // Vérifier si le bâtiment existe
  db.get('SELECT * FROM buildings WHERE id = ?', [buildingId], (err, building) => {
    if (err) {
      console.error('Erreur lors de la vérification du bâtiment:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!building) {
      return res.status(404).json({ error: 'Bâtiment non trouvé' });
    }
    
    // Vérifier si l'appartement existe déjà
    db.get(
      'SELECT * FROM apartments WHERE building_id = ? AND number = ?', 
      [buildingId, number], 
      (err, existingApt) => {
        if (err) {
          console.error('Erreur lors de la vérification de l\'appartement:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        if (existingApt) {
          return res.status(409).json({ error: 'Cet appartement existe déjà dans ce bâtiment' });
        }
        
        // Ajouter l'appartement
        db.run(
          'INSERT INTO apartments (building_id, number, floor) VALUES (?, ?, ?)',
          [buildingId, number, floor || null],
          function(err) {
            if (err) {
              console.error('Erreur lors de l\'ajout de l\'appartement:', err);
              return res.status(500).json({ error: 'Erreur serveur' });
            }
            
            // Récupérer l'appartement créé
            db.get('SELECT * FROM apartments WHERE id = ?', [this.lastID], (err, apartment) => {
              if (err) {
                console.error('Erreur lors de la récupération de l\'appartement créé:', err);
                return res.status(500).json({ error: 'Erreur serveur' });
              }
              
              res.status(201).json(apartment);
            });
          }
        );
      }
    );
  });
});

/**
 * Met à jour un bâtiment
 * PUT /api/buildings/:id
 */
router.put('/:id', (req, res) => {
  const buildingId = req.params.id;
  const { name, address, code } = req.body;
  
  // Vérifier si au moins un champ à modifier est fourni
  if (!name && !address && !code) {
    return res.status(400).json({ error: 'Aucune donnée à mettre à jour' });
  }
  
  // Construire la requête de mise à jour
  const updateFields = [];
  const values = [];
  
  if (name) {
    updateFields.push('name = ?');
    values.push(name);
  }
  
  if (address) {
    updateFields.push('address = ?');
    values.push(address);
  }
  
  if (code) {
    updateFields.push('code = ?');
    values.push(code);
  }
  
  // Ajouter l'ID du bâtiment aux valeurs
  values.push(buildingId);
  
  const query = `
    UPDATE buildings
    SET ${updateFields.join(', ')}
    WHERE id = ?
  `;
  
  db.run(query, values, function(err) {
    if (err) {
      console.error('Erreur lors de la mise à jour du bâtiment:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (this.changes === 0) {
      return res.status(404).json({ error: 'Bâtiment non trouvé ou aucune modification effectuée' });
    }
    
    // Récupérer le bâtiment mis à jour
    db.get('SELECT * FROM buildings WHERE id = ?', [buildingId], (err, updatedBuilding) => {
      if (err) {
        console.error('Erreur lors de la récupération du bâtiment mis à jour:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      res.json(updatedBuilding);
    });
  });
});

/**
 * Supprime un bâtiment
 * DELETE /api/buildings/:id
 */
router.delete('/:id', (req, res) => {
  const buildingId = req.params.id;
  
  // Vérifier si le bâtiment existe
  db.get('SELECT * FROM buildings WHERE id = ?', [buildingId], (err, building) => {
    if (err) {
      console.error('Erreur lors de la vérification du bâtiment:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!building) {
      return res.status(404).json({ error: 'Bâtiment non trouvé' });
    }
    
    // Vérifier s'il y a des incidents liés à ce bâtiment
    db.get('SELECT COUNT(*) as count FROM incidents WHERE building_id = ?', [buildingId], (err, result) => {
      if (err) {
        console.error('Erreur lors de la vérification des incidents:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (result.count > 0) {
        return res.status(409).json({ 
          error: 'Impossible de supprimer ce bâtiment car des incidents y sont associés' 
        });
      }
      
      // Supprimer les appartements associés
      db.run('DELETE FROM apartments WHERE building_id = ?', [buildingId], (err) => {
        if (err) {
          console.error('Erreur lors de la suppression des appartements:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        // Supprimer le bâtiment
        db.run('DELETE FROM buildings WHERE id = ?', [buildingId], function(err) {
          if (err) {
            console.error('Erreur lors de la suppression du bâtiment:', err);
            return res.status(500).json({ error: 'Erreur serveur' });
          }
          
          if (this.changes === 0) {
            return res.status(404).json({ error: 'Bâtiment non trouvé' });
          }
          
          res.status(200).json({ message: 'Bâtiment supprimé avec succès' });
        });
      });
    });
  });
});

module.exports = router; 