// website/routes/auth.js
// Authentication routes for registration, login, session validation (SQLite & Cloudflare D1 compatible)

const express = require('express');
const router = express.Router();
const db = require('../services/db');

/**
 * Extract auth token from headers
 */
function extractToken(req) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return authHeader.substring(7);
  }
  return req.headers['x-auth-token'] || null;
}

/**
 * POST /api/auth/register
 */
router.post('/register', async (req, res) => {
  const { username, email, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ success: false, error: 'Username and password are required' });
  }

  if (username.length < 3) {
    return res.status(400).json({ success: false, error: 'Username must be at least 3 characters' });
  }

  if (password.length < 6) {
    return res.status(400).json({ success: false, error: 'Password must be at least 6 characters' });
  }

  try {
    const user = await db.registerUser(username.trim(), email ? email.trim() : null, password);
    const token = await db.createSession(user.id);
    const stats = await db.getUserStats(user.id);
    const settings = await db.getUserSettings(user.id);

    res.json({
      success: true,
      token,
      user,
      data: {
        user,
        token,
        stats,
        settings
      }
    });
  } catch (err) {
    res.status(400).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/auth/login
 */
router.post('/login', async (req, res) => {
  const loginIdentity = req.body.username || req.body.login;
  const password = req.body.password;

  if (!loginIdentity || !password) {
    return res.status(400).json({ success: false, error: 'Username/email and password are required' });
  }

  try {
    const user = await db.verifyUser(loginIdentity.trim(), password);
    if (!user) {
      return res.status(401).json({ success: false, error: 'Invalid username/email or password' });
    }

    const token = await db.createSession(user.id);
    const stats = await db.getUserStats(user.id);
    const settings = await db.getUserSettings(user.id);

    res.json({
      success: true,
      token,
      user,
      data: {
        user,
        token,
        stats,
        settings
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/auth/demo
 * Instant 1-click Demo Login for Alex Mercer
 */
router.post('/demo', async (req, res) => {
  try {
    const user = await db.verifyUser('Alex Mercer', 'demo123');
    if (!user) {
      return res.status(500).json({ success: false, error: 'Demo user not seeded' });
    }

    const token = await db.createSession(user.id);
    const stats = await db.getUserStats(user.id);
    const settings = await db.getUserSettings(user.id);

    res.json({
      success: true,
      token,
      user,
      data: {
        user,
        token,
        stats,
        settings
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * GET /api/auth/me
 */
router.get('/me', async (req, res) => {
  const token = extractToken(req);
  if (!token) {
    return res.status(401).json({ success: false, error: 'No authorization token provided' });
  }

  try {
    const user = await db.getUserByToken(token);
    if (!user) {
      return res.status(401).json({ success: false, error: 'Session expired or invalid' });
    }

    const stats = await db.getUserStats(user.id);
    const settings = await db.getUserSettings(user.id);

    res.json({
      success: true,
      user,
      data: {
        user,
        stats,
        settings
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * POST /api/auth/logout
 */
router.post('/logout', async (req, res) => {
  const token = extractToken(req);
  if (token) {
    await db.removeSession(token);
  }
  res.json({ success: true, message: 'Logged out successfully' });
});

/**
 * GET /api/auth/accounts
 */
router.get('/accounts', async (req, res) => {
  try {
    const accounts = await db.getAllUsers();
    res.json({ success: true, accounts });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

/**
 * TV Code In-Memory / DB storage helper
 */
const tvPairCodes = new Map();

/**
 * POST /api/auth/tv-code/generate
 */
router.post('/tv-code/generate', async (req, res) => {
  const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  let code = '';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  const now = Date.now();
  const expiresAt = now + (10 * 60 * 1000);
  tvPairCodes.set(code, { status: 'pending', expiresAt, userId: null, token: null });

  res.json({ success: true, code, expiresIn: 600, expiresAt });
});

/**
 * GET /api/auth/tv-code/status
 */
router.get('/tv-code/status', async (req, res) => {
  const code = String(req.query.code || '').trim().toUpperCase();
  if (!code) return res.status(400).json({ success: false, error: 'Code is required' });

  const pairRow = tvPairCodes.get(code);
  const now = Date.now();
  if (!pairRow || pairRow.expiresAt < now) {
    return res.status(404).json({ success: false, status: 'expired', error: 'Code expired or not found' });
  }

  if (pairRow.status === 'paired' && pairRow.userId && pairRow.token) {
    try {
      const user = await db.getUserById(pairRow.userId);
      const settings = await db.getUserSettings(user.id);
      const stats = await db.getUserStats(user.id);
      return res.json({
        success: true,
        status: 'paired',
        token: pairRow.token,
        user,
        data: { user, token: pairRow.token, settings, stats }
      });
    } catch (err) {
      return res.status(500).json({ success: false, error: err.message });
    }
  }

  res.json({ success: true, status: 'pending' });
});

/**
 * POST /api/auth/tv-code/pair
 */
router.post('/tv-code/pair', async (req, res) => {
  const token = extractToken(req);
  if (!token) return res.status(401).json({ success: false, error: 'Unauthorized' });

  try {
    const user = await db.getUserByToken(token);
    if (!user) return res.status(401).json({ success: false, error: 'Session expired' });

    const code = String(req.body.code || '').trim().toUpperCase().replace(/[\s-]/g, '');
    const pairRow = tvPairCodes.get(code);
    const now = Date.now();

    if (!pairRow || pairRow.expiresAt < now) {
      return res.status(404).json({ success: false, error: 'Invalid or expired TV pairing code' });
    }

    const tvToken = await db.createSession(user.id);
    tvPairCodes.set(code, { status: 'paired', expiresAt: pairRow.expiresAt, userId: user.id, token: tvToken });

    res.json({ success: true, message: `TV linked to ${user.username}!`, user });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

module.exports = router;
