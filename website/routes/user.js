// website/routes/user.js
// User account endpoints: History, Watchlist, Playback Settings, Profile Stats (SQLite & Cloudflare D1 compatible)

const express = require('express');
const router = express.Router();
const db = require('../services/db');

/**
 * Middleware: Verify user authentication
 */
async function requireAuth(req, res, next) {
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

  try {
    const user = await db.getUserByToken(token);
    if (!user) {
      return res.status(401).json({ success: false, error: 'Session expired or invalid' });
    }

    req.user = user;
    next();
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
}

/**
 * GET /api/user/profile
 */
router.get('/profile', requireAuth, async (req, res) => {
  try {
    const stats = await db.getUserStats(req.user.id);
    const settings = await db.getUserSettings(req.user.id);
    res.json({
      success: true,
      data: {
        user: req.user,
        stats,
        settings
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /api/user/history
 */
router.get('/history', requireAuth, async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 50;
    const history = await db.getWatchHistory(req.user.id, limit);
    res.json({ success: true, data: history });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/user/history
 * Save or update watch progress
 */
router.post('/history', requireAuth, async (req, res) => {
  const { animeId, animeTitle, poster, episodeNum, episodeId, progressSeconds, durationSeconds } = req.body;

  if (!animeId) {
    return res.status(400).json({ success: false, error: 'animeId is required' });
  }

  try {
    await db.saveWatchHistory(req.user.id, {
      animeId,
      animeTitle,
      poster,
      episodeNum,
      episodeId,
      progressSeconds,
      durationSeconds
    });

    res.json({ success: true, message: 'History updated' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * DELETE /api/user/history/:animeId
 */
router.delete('/history/:animeId', requireAuth, async (req, res) => {
  try {
    await db.deleteHistoryItem(req.user.id, req.params.animeId);
    res.json({ success: true, message: 'Item removed from history' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * DELETE /api/user/history
 * Clear entire history
 */
router.delete('/history', requireAuth, async (req, res) => {
  try {
    await db.clearWatchHistory(req.user.id);
    res.json({ success: true, message: 'History cleared' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /api/user/watchlist
 */
router.get('/watchlist', requireAuth, async (req, res) => {
  try {
    const status = req.query.status || null;
    const list = await db.getWatchlist(req.user.id, status);
    res.json({ success: true, data: list });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/user/watchlist
 */
router.post('/watchlist', requireAuth, async (req, res) => {
  const { animeId, animeTitle, poster, format, score, status } = req.body;

  if (!animeId) {
    return res.status(400).json({ success: false, error: 'animeId is required' });
  }

  try {
    await db.addToWatchlist(req.user.id, {
      animeId,
      animeTitle,
      poster,
      format,
      score,
      status: status || 'Watching'
    });

    res.json({ success: true, message: 'Watchlist updated' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * DELETE /api/user/watchlist/:animeId
 */
router.delete('/watchlist/:animeId', requireAuth, async (req, res) => {
  try {
    await db.removeFromWatchlist(req.user.id, req.params.animeId);
    res.json({ success: true, message: 'Removed from watchlist' });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /api/user/watchlist/check/:animeId
 */
router.get('/watchlist/check/:animeId', requireAuth, async (req, res) => {
  try {
    const row = await db.isWatchlisted(req.user.id, req.params.animeId);
    res.json({ success: true, isWatchlisted: !!row, status: row ? row.status : null });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /api/user/settings
 */
router.get('/settings', requireAuth, async (req, res) => {
  try {
    const settings = await db.getUserSettings(req.user.id);
    res.json({ success: true, data: settings });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/user/settings or PUT /api/user/settings
 */
router.post('/settings', requireAuth, async (req, res) => {
  try {
    const settings = await db.updateUserSettings(req.user.id, req.body);
    res.json({ success: true, data: settings });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.put('/settings', requireAuth, async (req, res) => {
  try {
    const settings = await db.updateUserSettings(req.user.id, req.body);
    res.json({ success: true, data: settings });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
