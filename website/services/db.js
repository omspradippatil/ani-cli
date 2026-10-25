// website/services/db.js
// Embedded SQLite Database Service for Om CLI using Node 24 native node:sqlite

const { DatabaseSync } = require('node:sqlite');
const path = require('path');
const crypto = require('node:crypto');

const dbPath = path.join(__dirname, '../data/kuronami.db');
const db = new DatabaseSync(dbPath);

// Initialize Tables
db.exec(`
  PRAGMA journal_mode = WAL;
  
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    avatar TEXT,
    tier TEXT DEFAULT 'Community Otaku',
    level INTEGER DEFAULT 42,
    created_at INTEGER NOT NULL
  );

  CREATE TABLE IF NOT EXISTS sessions (
    token TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS watch_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    anime_id TEXT NOT NULL,
    anime_title TEXT NOT NULL,
    poster TEXT,
    episode_num REAL NOT NULL,
    episode_id TEXT NOT NULL,
    progress_seconds REAL DEFAULT 0,
    duration_seconds REAL DEFAULT 0,
    updated_at INTEGER NOT NULL,
    UNIQUE(user_id, anime_id)
  );

  CREATE TABLE IF NOT EXISTS watchlist (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    anime_id TEXT NOT NULL,
    anime_title TEXT NOT NULL,
    poster TEXT,
    format TEXT DEFAULT 'TV',
    score REAL DEFAULT 0,
    status TEXT DEFAULT 'Watching',
    updated_at INTEGER NOT NULL,
    UNIQUE(user_id, anime_id)
  );

  CREATE TABLE IF NOT EXISTS user_settings (
    user_id INTEGER PRIMARY KEY,
    preferred_audio TEXT DEFAULT 'sub',
    auto_next INTEGER DEFAULT 1,
    auto_skip_intro INTEGER DEFAULT 1,
    auto_play INTEGER DEFAULT 1,
    preferred_quality TEXT DEFAULT 'auto',
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
  );
`);

// Sanitize any legacy tier names to completely free Community Otaku
try {
  db.exec(`
    UPDATE users SET tier = 'Community Otaku' WHERE tier LIKE '%VIP%' OR tier IS NULL;
    UPDATE users SET email = 'alex@omcli.io' WHERE email = 'alex@kuronami.io';
  `);
} catch (e) {}

/**
 * Hash password with salt
 */
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');
  return { hash, salt };
}

/**
 * Seed default demo user if empty
 */
function seedDemoUser() {
  const countRow = db.prepare('SELECT COUNT(*) as count FROM users').get();
  if (countRow.count === 0) {
    const { hash, salt } = hashPassword('demo123');
    const now = Date.now();
    const result = db.prepare(`
      INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `).run(
      'Alex Mercer',
      'alex@omcli.io',
      hash,
      salt,
      'https://lh3.googleusercontent.com/aida-public/AB6AXuDTN9AsbPHoQqZimkdf5SKxBvZavvxOhDlT8E9vCHRzGbpe1hvxThzKYFlNmpGAueS_ucTqnCwn48iM-L9lqJQFrldtyx9Xp6-ZCo3-27ltcXs134z1k-7sAYvECqoB3KAEdg54dq4IeYszVKZ2P-tUZLVcDT7_3WqhYcw-qmMiNH1qu3LL-qQ12-c0cyUrSwMX7Zl5uNLBTk7kzuIME_SzDxIEadw5iosTQIcUEh-p',
      'Community Otaku',
      42,
      now
    );

    const demoUserId = Number(result.lastInsertRowid);
    db.prepare(`
      INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
      VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
    `).run(demoUserId, now);

    // Seed some initial history
    db.prepare(`
      INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
      VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 1, '22676', 740, 1420, ?)
    `).run(demoUserId, now - 3600000);

    // Seed initial watchlist
    db.prepare(`
      INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
      VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 'TV', 7.9, 'Watching', ?)
    `).run(demoUserId, now - 7200000);

    db.prepare(`
      INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
      VALUES (?, '101922', 'Demon Slayer: Kimetsu no Yaiba', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-PEn1CTDYxZt2.png', 'TV', 8.5, 'Watching', ?)
    `).run(demoUserId, now - 3600000);

    console.log('[DB] Seeded demo user: Alex Mercer (username: Alex Mercer / pass: demo123)');
  }
}

seedDemoUser();

/**
 * Register a new user
 */
function registerUser(username, email, password) {
  const existing = db.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email);
  if (existing) {
    throw new Error('Username or email already exists');
  }

  const { hash, salt } = hashPassword(password);
  const now = Date.now();
  const avatar = `https://api.dicebear.com/7.x/bottts-neutral/svg?seed=${encodeURIComponent(username)}`;

  const res = db.prepare(`
    INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
    VALUES (?, ?, ?, ?, ?, 'Standard Otaku', 1, ?)
  `).run(username, email, hash, salt, avatar, now);

  const userId = Number(res.lastInsertRowid);
  db.prepare(`
    INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
    VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
  `).run(userId, now);

  return getUserById(userId);
}

/**
 * Verify login credentials
 */
function verifyUser(usernameOrEmail, password) {
  const user = db.prepare('SELECT * FROM users WHERE username = ? OR email = ?').get(usernameOrEmail, usernameOrEmail);
  if (!user) return null;

  const { hash } = hashPassword(password, user.salt);
  if (hash !== user.password_hash) return null;

  return getUserById(user.id);
}

/**
 * Get user sanitized by ID
 */
function getUserById(id) {
  const user = db.prepare('SELECT id, username, email, avatar, tier, level, created_at FROM users WHERE id = ?').get(id);
  return user || null;
}

/**
 * Create session token (valid 30 days)
 */
function createSession(userId) {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000);
  db.prepare('INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)').run(token, userId, expiresAt);
  return token;
}

/**
 * Get user by session token
 */
function getUserByToken(token) {
  if (!token) return null;
  const session = db.prepare('SELECT * FROM sessions WHERE token = ? AND expires_at > ?').get(token, Date.now());
  if (!session) return null;
  return getUserById(session.user_id);
}

/**
 * Invalidate session token
 */
function removeSession(token) {
  if (!token) return;
  db.prepare('DELETE FROM sessions WHERE token = ?').run(token);
}

/**
 * History methods
 */
function saveWatchHistory(userId, data) {
  const now = Date.now();
  db.prepare(`
    INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, anime_id) DO UPDATE SET
      episode_num = excluded.episode_num,
      episode_id = excluded.episode_id,
      progress_seconds = excluded.progress_seconds,
      duration_seconds = excluded.duration_seconds,
      updated_at = excluded.updated_at
  `).run(
    userId,
    String(data.animeId),
    data.animeTitle || 'Anime',
    data.poster || '',
    Number(data.episodeNum) || 1,
    String(data.episodeId || ''),
    Number(data.progressSeconds) || 0,
    Number(data.durationSeconds) || 0,
    now
  );
}

function getWatchHistory(userId, limit = 50) {
  return db.prepare(`
    SELECT * FROM watch_history 
    WHERE user_id = ? 
    ORDER BY updated_at DESC 
    LIMIT ?
  `).all(userId, limit);
}

function deleteHistoryItem(userId, animeId) {
  db.prepare('DELETE FROM watch_history WHERE user_id = ? AND anime_id = ?').run(userId, String(animeId));
}

function clearWatchHistory(userId) {
  db.prepare('DELETE FROM watch_history WHERE user_id = ?').run(userId);
}

/**
 * Watchlist methods
 */
function addToWatchlist(userId, data) {
  const now = Date.now();
  db.prepare(`
    INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, anime_id) DO UPDATE SET
      status = excluded.status,
      updated_at = excluded.updated_at
  `).run(
    userId,
    String(data.animeId),
    data.animeTitle || 'Anime',
    data.poster || '',
    data.format || 'TV',
    Number(data.score) || 0,
    data.status || 'Watching',
    now
  );
}

function getWatchlist(userId, statusFilter = null) {
  if (statusFilter && statusFilter !== 'All') {
    return db.prepare(`
      SELECT * FROM watchlist 
      WHERE user_id = ? AND status = ?
      ORDER BY updated_at DESC
    `).all(userId, statusFilter);
  }
  return db.prepare(`
    SELECT * FROM watchlist 
    WHERE user_id = ?
    ORDER BY updated_at DESC
  `).all(userId);
}

function removeFromWatchlist(userId, animeId) {
  db.prepare('DELETE FROM watchlist WHERE user_id = ? AND anime_id = ?').run(userId, String(animeId));
}

function isWatchlisted(userId, animeId) {
  const row = db.prepare('SELECT id, status FROM watchlist WHERE user_id = ? AND anime_id = ?').get(userId, String(animeId));
  return row || null;
}

/**
 * User Settings
 */
function getUserSettings(userId) {
  let settings = db.prepare('SELECT * FROM user_settings WHERE user_id = ?').get(userId);
  if (!settings) {
    const now = Date.now();
    db.prepare(`
      INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
      VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
    `).run(userId, now);
    settings = db.prepare('SELECT * FROM user_settings WHERE user_id = ?').get(userId);
  }
  return settings;
}

function updateUserSettings(userId, data) {
  const now = Date.now();
  db.prepare(`
    UPDATE user_settings 
    SET preferred_audio = COALESCE(?, preferred_audio),
        auto_next = COALESCE(?, auto_next),
        auto_skip_intro = COALESCE(?, auto_skip_intro),
        auto_play = COALESCE(?, auto_play),
        preferred_quality = COALESCE(?, preferred_quality),
        updated_at = ?
    WHERE user_id = ?
  `).run(
    data.preferredAudio,
    data.autoNext !== undefined ? (data.autoNext ? 1 : 0) : null,
    data.autoSkipIntro !== undefined ? (data.autoSkipIntro ? 1 : 0) : null,
    data.autoPlay !== undefined ? (data.autoPlay ? 1 : 0) : null,
    data.preferredQuality,
    now,
    userId
  );
  return getUserSettings(userId);
}

/**
 * Aggregate User Stats
 */
function getUserStats(userId) {
  const totalEpisodesRow = db.prepare(`
    SELECT COUNT(*) as total_entries, SUM(episode_num) as sum_episodes, SUM(progress_seconds) as total_seconds
    FROM watch_history WHERE user_id = ?
  `).get(userId);

  const completedCountRow = db.prepare(`
    SELECT COUNT(*) as count FROM watchlist WHERE user_id = ? AND status = 'Completed'
  `).get(userId);

  const watchlistCountRow = db.prepare(`
    SELECT COUNT(*) as count FROM watchlist WHERE user_id = ?
  `).get(userId);

  const totalHours = Math.round((totalEpisodesRow.total_seconds || 0) / 3600);

  return {
    episodesWatched: totalEpisodesRow.sum_episodes || totalEpisodesRow.total_entries || 0,
    watchTimeHours: totalHours > 0 ? totalHours : Math.round((totalEpisodesRow.total_entries || 0) * 0.4),
    completedCount: completedCountRow.count || 0,
    watchlistCount: watchlistCountRow.count || 0
  };
}

module.exports = {
  registerUser,
  verifyUser,
  getUserById,
  createSession,
  getUserByToken,
  removeSession,
  saveWatchHistory,
  getWatchHistory,
  deleteHistoryItem,
  clearWatchHistory,
  addToWatchlist,
  getWatchlist,
  removeFromWatchlist,
  isWatchlisted,
  getUserSettings,
  updateUserSettings,
  getUserStats
};
