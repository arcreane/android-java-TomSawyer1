const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const { db } = require('../database');

// Configuration de Multer pour le stockage des fichiers
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, path.join(__dirname, '../uploads/'));
  },
  filename: function (req, file, cb) {
    // Vérifier si le nom du fichier original contient des informations d'identification
    if (file.originalname.includes('incident_') || file.originalname.includes('user_')) {
      // Préserver le nom du fichier original qui contient déjà des informations d'identification
      cb(null, file.originalname);
    } else {
      // Générer un nom de fichier unique pour les anciennes versions de l'app
      const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
      const extension = path.extname(file.originalname);
      cb(null, 'image-' + uniqueSuffix + extension);
    }
  }
});

// Filtrer les types de fichiers autorisés
const fileFilter = (req, file, cb) => {
  // Accepter uniquement les images
  if (file.mimetype.startsWith('image/')) {
    cb(null, true);
  } else {
    cb(new Error('Seules les images sont autorisées'), false);
  }
};

// Initialiser multer avec notre configuration
const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // Limiter à 5MB
  },
  fileFilter: fileFilter
});

/**
 * Upload d'image pour un incident
 * POST /api/uploads/incident-image
 */
router.post('/incident-image', upload.single('image'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: 'Aucune image n\'a été téléchargée' });
  }

  // Obtenir le chemin relatif pour stocker dans la base de données
  const relativePath = `/uploads/${req.file.filename}`;

  // Renvoyer le chemin de l'image
  res.status(201).json({
    success: true,
    imagePath: relativePath,
    fileName: req.file.filename,
    size: req.file.size
  });
});

/**
 * Récupération d'une image par son nom de fichier
 * GET /api/uploads/:filename
 */
router.get('/:filename', (req, res) => {
  const filename = req.params.filename;
  const imagePath = path.join(__dirname, '../uploads/', filename);

  // Vérifier si le fichier existe
  if (fs.existsSync(imagePath)) {
    // Envoyer le fichier
    res.sendFile(imagePath);
  } else {
    res.status(404).json({ error: 'Image non trouvée' });
  }
});

/**
 * Supprimer une image
 * DELETE /api/uploads/:filename
 */
router.delete('/:filename', (req, res) => {
  const filename = req.params.filename;
  const imagePath = path.join(__dirname, '../uploads/', filename);

  // Vérifier si le fichier existe
  if (fs.existsSync(imagePath)) {
    // Supprimer le fichier
    fs.unlink(imagePath, (err) => {
      if (err) {
        console.error('Erreur lors de la suppression de l\'image:', err);
        return res.status(500).json({ error: 'Erreur serveur' });
      }
      res.status(200).json({ message: 'Image supprimée avec succès' });
    });
  } else {
    res.status(404).json({ error: 'Image non trouvée' });
  }
});

module.exports = router; 