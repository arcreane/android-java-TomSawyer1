const express = require('express');
const router = express.Router();
const { db } = require('../database');
const crypto = require('crypto');

// Fonction utilitaire pour générer un code de suivi aléatoire
function generateTrackingCode() {
  // Générer une chaîne aléatoire de 8 caractères alphanumériques
  return crypto.randomBytes(4).toString('hex').toUpperCase();
}

/**
 * Récupère tous les incidents
 * GET /api/incidents
 */
router.get('/', (req, res) => {
  const query = `
    SELECT i.*, b.name as building_name, u1.fullname as reporter_name, u2.fullname as assignee_name 
    FROM incidents i
    LEFT JOIN buildings b ON i.building_id = b.id
    LEFT JOIN users u1 ON i.reported_by = u1.id
    LEFT JOIN users u2 ON i.assigned_to = u2.id
    ORDER BY i.created_at DESC
  `;
  
  db.all(query, [], (err, rows) => {
    if (err) {
      console.error('Erreur lors de la récupération des incidents:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    res.json(rows);
  });
});

/**
 * Récupère les commentaires d'un incident
 * GET /api/incidents/:id/comments
 */
router.get('/:id/comments', (req, res) => {
  const incidentId = req.params.id;
  db.all(
    `SELECT c.*, u.fullname as user_name FROM comments c JOIN users u ON c.user_id = u.id WHERE c.incident_id = ? ORDER BY c.created_at ASC`,
    [incidentId],
    (err, comments) => {
      if (err) {
        console.error('Erreur lors de la récupération des commentaires:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      res.json(comments);
    }
  );
});

/**
 * Récupère un incident par son ID
 * GET /api/incidents/:id
 */
router.get('/:id', (req, res) => {
  const incidentId = req.params.id;
  
  const query = `
    SELECT i.*, b.name as building_name, u1.fullname as reporter_name, u2.fullname as assignee_name 
    FROM incidents i
    LEFT JOIN buildings b ON i.building_id = b.id
    LEFT JOIN users u1 ON i.reported_by = u1.id
    LEFT JOIN users u2 ON i.assigned_to = u2.id
    WHERE i.id = ?
  `;
  
  db.get(query, [incidentId], (err, incident) => {
    if (err) {
      console.error('Erreur lors de la récupération de l\'incident:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!incident) {
      return res.status(404).json({ error: 'Incident non trouvé' });
    }
    
    // Ajouter l'URL complète de l'image si elle existe
    if (incident.image_path) {
      incident.image_url = `${req.protocol}://${req.get('host')}${incident.image_path}`;
    }
    
    // Récupérer les commentaires associés à l'incident
    const commentsQuery = `
      SELECT c.*, u.fullname as user_name
      FROM comments c
      JOIN users u ON c.user_id = u.id
      WHERE c.incident_id = ?
      ORDER BY c.created_at ASC
    `;
    
    db.all(commentsQuery, [incidentId], (err, comments) => {
      if (err) {
        console.error('Erreur lors de la récupération des commentaires:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      // Ajouter les commentaires à l'objet incident
      incident.comments = comments;
      
      res.json(incident);
    });
  });
});

/**
 * Récupère un incident par son code de suivi
 * GET /api/incidents/track/:code
 */
router.get('/track/:code', (req, res) => {
  const trackingCode = req.params.code;
  
  const query = `
    SELECT i.*, b.name as building_name, u1.fullname as reporter_name, u2.fullname as assignee_name 
    FROM incidents i
    LEFT JOIN buildings b ON i.building_id = b.id
    LEFT JOIN users u1 ON i.reported_by = u1.id
    LEFT JOIN users u2 ON i.assigned_to = u2.id
    WHERE i.tracking_code = ?
  `;
  
  db.get(query, [trackingCode], (err, incident) => {
    if (err) {
      console.error('Erreur lors de la recherche de l\'incident:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!incident) {
      return res.status(404).json({ error: 'Incident non trouvé' });
    }
    
    res.json(incident);
  });
});

/**
 * Récupère un incident par son code de suivi
 * GET /api/incidents/tracking/:code
 */
router.get('/tracking/:code', (req, res) => {
  const trackingCode = req.params.code;
  const query = `
    SELECT i.*, b.name as building_name, u1.fullname as reporter_name, u2.fullname as assignee_name 
    FROM incidents i
    LEFT JOIN buildings b ON i.building_id = b.id
    LEFT JOIN users u1 ON i.reported_by = u1.id
    LEFT JOIN users u2 ON i.assigned_to = u2.id
    WHERE i.tracking_code = ?
  `;
  db.get(query, [trackingCode], (err, incident) => {
    if (err) return res.status(500).json({ error: 'Erreur serveur' });
    if (!incident) return res.status(404).json({ error: 'Incident non trouvé' });
    res.json(incident);
  });
});

/**
 * Crée un nouvel incident
 * POST /api/incidents
 */
router.post('/', (req, res) => {
  const {
    title,
    description,
    priority,
    location,
    apartment_id,
    building_id,
    reported_by,
    image_path,
    email
  } = req.body;
  
  // Vérification des champs obligatoires
  if (!title || !description || !priority || !building_id || !reported_by) {
    return res.status(400).json({ error: 'Champs obligatoires manquants' });
  }
  
  // Générer un code de suivi unique
  const trackingCode = generateTrackingCode();
  
  const query = `
    INSERT INTO incidents (
      title, description, status, priority, location, tracking_code, apartment_id,
      building_id, reported_by, image_path, email
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `;
  
  db.run(
    query,
    [title, description, 'pending', priority, location, trackingCode, apartment_id || null, building_id, reported_by, image_path || null, email || null],
    function(err) {
      if (err) {
        console.error('Erreur lors de la création de l\'incident:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      // Récupérer l'incident créé avec informations supplémentaires
      const incidentQuery = `
        SELECT i.*, b.name as building_name, u.fullname as reporter_name 
        FROM incidents i
        LEFT JOIN buildings b ON i.building_id = b.id
        LEFT JOIN users u ON i.reported_by = u.id
        WHERE i.id = ?
      `;
      
      db.get(incidentQuery, [this.lastID], (err, incident) => {
        if (err) {
          console.error('Erreur lors de la récupération de l\'incident créé:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        // Ajouter l'URL complète de l'image si elle existe
        if (incident.image_path) {
          incident.image_url = `${req.protocol}://${req.get('host')}${incident.image_path}`;
        }
        
        res.status(201).json(incident);
      });
    }
  );
});

/**
 * Met à jour un incident
 * PUT /api/incidents/:id
 */
router.put('/:id', (req, res) => {
  const incidentId = req.params.id;
  const {
    title,
    description,
    status,
    priority,
    location,
    apartment_id,
    assigned_to
  } = req.body;
  
  // Vérifier si l'incident existe
  db.get('SELECT * FROM incidents WHERE id = ?', [incidentId], (err, incident) => {
    if (err) {
      console.error('Erreur lors de la vérification de l\'incident:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!incident) {
      return res.status(404).json({ error: 'Incident non trouvé' });
    }
    
    // Construire la requête de mise à jour
    const updateFields = [];
    const values = [];
    
    if (title) {
      updateFields.push('title = ?');
      values.push(title);
    }
    
    if (description) {
      updateFields.push('description = ?');
      values.push(description);
    }
    
    if (status) {
      updateFields.push('status = ?');
      values.push(status);
    }
    
    if (priority) {
      updateFields.push('priority = ?');
      values.push(priority);
    }
    
    if (location !== undefined) {
      updateFields.push('location = ?');
      values.push(location);
    }
    
    if (apartment_id !== undefined) {
      updateFields.push('apartment_id = ?');
      values.push(apartment_id);
    }
    
    if (assigned_to !== undefined) {
      updateFields.push('assigned_to = ?');
      values.push(assigned_to);
    }
    
    updateFields.push('updated_at = CURRENT_TIMESTAMP');
    
    // S'il n'y a rien à mettre à jour
    if (updateFields.length === 1) {
      return res.status(400).json({ error: 'Aucun champ à mettre à jour' });
    }
    
    // Ajouter l'ID de l'incident aux valeurs
    values.push(incidentId);
    
    const query = `
      UPDATE incidents
      SET ${updateFields.join(', ')}
      WHERE id = ?
    `;
    
    db.run(query, values, function(err) {
      if (err) {
        console.error('Erreur lors de la mise à jour de l\'incident:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      if (this.changes === 0) {
        return res.status(404).json({ error: 'Incident non trouvé ou aucune modification effectuée' });
      }
      
      // Récupérer l'incident mis à jour
      db.get('SELECT * FROM incidents WHERE id = ?', [incidentId], (err, updatedIncident) => {
        if (err) {
          console.error('Erreur lors de la récupération de l\'incident mis à jour:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        res.json(updatedIncident);
      });
    });
  });
});

/**
 * Supprime un incident
 * DELETE /api/incidents/:id
 */
router.delete('/:id', (req, res) => {
  const incidentId = req.params.id;
  
  // Vérifier si l'incident existe
  db.get('SELECT * FROM incidents WHERE id = ?', [incidentId], (err, incident) => {
    if (err) {
      console.error('Erreur lors de la vérification de l\'incident:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!incident) {
      return res.status(404).json({ error: 'Incident non trouvé' });
    }
    
    // Supprimer tous les commentaires associés à l'incident
    db.run('DELETE FROM comments WHERE incident_id = ?', [incidentId], (err) => {
      if (err) {
        console.error('Erreur lors de la suppression des commentaires:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      
      // Supprimer l'incident
      db.run('DELETE FROM incidents WHERE id = ?', [incidentId], function(err) {
        if (err) {
          console.error('Erreur lors de la suppression de l\'incident:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        if (this.changes === 0) {
          return res.status(404).json({ error: 'Incident non trouvé' });
        }
        
        res.status(200).json({ message: 'Incident supprimé avec succès' });
      });
    });
  });
});

/**
 * Ajoute un commentaire à un incident
 * POST /api/incidents/:id/comments
 */
router.post('/:id/comments', (req, res) => {
  const incidentId = req.params.id;
  const { user_id, comment } = req.body;
  
  if (!user_id || !comment) {
    return res.status(400).json({ error: 'Champs obligatoires manquants' });
  }
  
  // Vérifier si l'incident existe
  db.get('SELECT * FROM incidents WHERE id = ?', [incidentId], (err, incident) => {
    if (err) {
      console.error('Erreur lors de la vérification de l\'incident:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    if (!incident) {
      return res.status(404).json({ error: 'Incident non trouvé' });
    }
    
    // Ajouter le commentaire
    db.run(
      'INSERT INTO comments (incident_id, user_id, comment) VALUES (?, ?, ?)',
      [incidentId, user_id, comment],
      function(err) {
        if (err) {
          console.error('Erreur lors de l\'ajout du commentaire:', err);
          return res.status(500).json({ error: 'Erreur serveur' });
        }
        
        // Récupérer le commentaire créé
        db.get(
          `SELECT c.*, u.fullname as user_name 
           FROM comments c 
           JOIN users u ON c.user_id = u.id 
           WHERE c.id = ?`,
          [this.lastID],
          (err, newComment) => {
            if (err) {
              console.error('Erreur lors de la récupération du commentaire:', err);
              return res.status(500).json({ error: 'Erreur serveur' });
            }
            
            res.status(201).json(newComment);
          }
        );
      }
    );
  });
});

module.exports = router; 