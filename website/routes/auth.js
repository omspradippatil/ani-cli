// website/routes/auth.js
// Authentication routes for registration, login, session validation

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
router.post('/register', (req, res) => {
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
    const user = db.registerUser(username.trim(), email ? email.trim() : null, password);
    const token = db.createSession(user.id);
    const stats = db.getUserStats(user.id);
    const settings = db.getUserSettings(user.id);

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
router.post('/login', (req, res) => {
  const loginIdentity = req.body.username || req.body.login;
  const password = req.body.password;

  if (!loginIdentity || !password) {
    return res.status(400).json({ success: false, error: 'Username/email and password are required' });
  }

  const user = db.verifyUser(loginIdentity.trim(), password);
  if (!user) {
    return res.status(401).json({ success: false, error: 'Invalid username/email or password' });
  }

  const token = db.createSession(user.id);
  const stats = db.getUserStats(user.id);
  const settings = db.getUserSettings(user.id);

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
});

/**
 * POST /api/auth/demo
 * Instant 1-click Demo Login for Alex Mercer
 */
router.post('/demo', (req, res) => {
  const user = db.verifyUser('Alex Mercer', 'demo123');
  if (!user) {
    return res.status(500).json({ success: false, error: 'Demo user not seeded' });
  }

  const token = db.createSession(user.id);
  const stats = db.getUserStats(user.id);
  const settings = db.getUserSettings(user.id);

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
});

/**
 * GET /api/auth/me
 */
router.get('/me', (req, res) => {
  const token = extractToken(req);
  if (!token) {
    return res.status(401).json({ success: false, error: 'No authorization token provided' });
  }

  const user = db.getUserByToken(token);
  if (!user) {
    return res.status(401).json({ success: false, error: 'Session expired or invalid' });
  }

  const stats = db.getUserStats(user.id);
  const settings = db.getUserSettings(user.id);

  res.json({
    success: true,
    user,
    data: {
      user,
      stats,
      settings
    }
  });
});

/**
 * POST /api/auth/logout
 */
router.post('/logout', (req, res) => {
  const token = extractToken(req);
  if (token) {
    db.removeSession(token);
  }
  res.json({ success: true, message: 'Logged out successfully' });
});

module.exports = router;
