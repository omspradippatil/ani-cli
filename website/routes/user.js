// website/routes/user.js
// User account endpoints: History, Watchlist, Playback Settings, Profile Stats

const express = require('express');
const router = express.Router();
const db = require('../services/db');

/**
 * Middleware: Verify user authentication
 */
function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  let token = null;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    token = authHeader.substring(7);
  } else {
    token = req.headers['x-auth-token'];
  }

  if (!token) {
    return res.status(401).json({ success: false, error: 'Authentication required' });
  }

  const user = db.getUserByToken(token);
  if (!user) {
    return res.status(401).json({ success: false, error: 'Session expired or invalid' });
  }

  req.user = user;
  next();
}

/**
 * GET /api/user/profile
 */
router.get('/profile', requireAuth, (req, res) => {
  const stats = db.getUserStats(req.user.id);
  const settings = db.getUserSettings(req.user.id);
  res.json({
    success: true,
    data: {
      user: req.user,
      stats,
      settings
    }
  });
});

/**
 * GET /api/user/history
 */
router.get('/history', requireAuth, (req, res) => {
  const limit = parseInt(req.query.limit) || 50;
  const history = db.getWatchHistory(req.user.id, limit);
  res.json({ success: true, data: history });
});

/**
 * POST /api/user/history
 * Save or update watch progress
 */
router.post('/history', requireAuth, (req, res) => {
  const { animeId, animeTitle, poster, episodeNum, episodeId, progressSeconds, durationSeconds } = req.body;

  if (!animeId) {
    return res.status(400).json({ success: false, error: 'animeId is required' });
  }

  db.saveWatchHistory(req.user.id, {
    animeId,
    animeTitle,
    poster,
    episodeNum,
    episodeId,
    progressSeconds,
    durationSeconds
  });

  res.json({ success: true, message: 'History updated' });
});

/**
 * DELETE /api/user/history/:animeId
 */
router.delete('/history/:animeId', requireAuth, (req, res) => {
  db.deleteHistoryItem(req.user.id, req.params.animeId);
  res.json({ success: true, message: 'Item removed from history' });
});

/**
 * DELETE /api/user/history
 * Clear entire history
 */
router.delete('/history', requireAuth, (req, res) => {
  db.clearWatchHistory(req.user.id);
  res.json({ success: true, message: 'History cleared' });
});

/**
 * GET /api/user/watchlist
 */
router.get('/watchlist', requireAuth, (req, res) => {
  const status = req.query.status || null;
  const list = db.getWatchlist(req.user.id, status);
  res.json({ success: true, data: list });
});

/**
 * POST /api/user/watchlist
 */
router.post('/watchlist', requireAuth, (req, res) => {
  const { animeId, animeTitle, poster, format, score, status } = req.body;

  if (!animeId) {
    return res.status(400).json({ success: false, error: 'animeId is required' });
  }

  db.addToWatchlist(req.user.id, {
    animeId,
    animeTitle,
    poster,
    format,
    score,
    status: status || 'Watching'
  });

  res.json({ success: true, message: 'Watchlist updated' });
});

/**
 * DELETE /api/user/watchlist/:animeId
 */
router.delete('/watchlist/:animeId', requireAuth, (req, res) => {
  db.removeFromWatchlist(req.user.id, req.params.animeId);
  res.json({ success: true, message: 'Removed from watchlist' });
});

/**
 * GET /api/user/watchlist/check/:animeId
 */
router.get('/watchlist/check/:animeId', requireAuth, (req, res) => {
  const row = db.isWatchlisted(req.user.id, req.params.animeId);
  res.json({ success: true, isWatchlisted: !!row, status: row ? row.status : null });
});

/**
 * GET /api/user/settings
 */
router.get('/settings', requireAuth, (req, res) => {
  const settings = db.getUserSettings(req.user.id);
  res.json({ success: true, data: settings });
});

/**
 * POST /api/user/settings or PUT /api/user/settings
 */
router.post('/settings', requireAuth, (req, res) => {
  const settings = db.updateUserSettings(req.user.id, req.body);
  res.json({ success: true, data: settings });
});

router.put('/settings', requireAuth, (req, res) => {
  const settings = db.updateUserSettings(req.user.id, req.body);
  res.json({ success: true, data: settings });
});

module.exports = router;
