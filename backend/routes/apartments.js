const express = require('express');
const router = express.Router();
const { db } = require('../database');

/**
 * Récupère tous les appartements, filtré par building_id si spécifié
 * GET /api/apartments?building_id=X
 */
router.get('/', (req, res) => {
  const buildingId = req.query.building_id;
  
  let query = `
    SELECT a.*, b.name as building_name
    FROM apartments a
    JOIN buildings b ON a.building_id = b.id
  `;
  
  const params = [];
  
  if (buildingId) {
    query += " WHERE a.building_id = ?";
    params.push(buildingId);
  }
  
  query += " ORDER BY a.floor, a.number";
  
  db.all(query, params, (err, apartments) => {
    if (err) {
      console.error('Erreur lors de la récupération des appartements:', err);
      return res.status(500).json({ error: 'Erreur serveur' });
    }
    
    // Log pour debug
    console.log(`Appartements récupérés pour le bâtiment ${buildingId || 'tous'}: ${apartments.length}`);
    apartments.forEach(apt => {
      console.log(`Apt #${apt.id} - Numéro: ${apt.number}, Étage: ${apt.floor}, Bâtiment: ${apt.building_name}`);
    });
    
    res.json(apartments);
  });
});

module.exports = router; 