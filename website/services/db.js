// website/services/db.js
// Resilient Database Service for Om CLI (Supports Node native node:sqlite, /tmp on Vercel, and in-memory fallback)

const fs = require('fs');
const path = require('path');
const crypto = require('node:crypto');

let db = null;
let useMemoryFallback = false;

// Determine writable DB path
const dbDir = process.env.VERCEL ? '/tmp' : path.join(__dirname, '../data');
const dbPath = path.join(dbDir, 'kuronami.db');

try {
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  const { DatabaseSync } = require('node:sqlite');
  db = new DatabaseSync(dbPath);

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

} catch (err) {
  console.warn('[DB] SQLite DatabaseSync unavailable on this runtime, using memory fallback:', err.message);
  useMemoryFallback = true;
}

/**
 * Hash password with salt
 */
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');
  return { hash, salt };
}

// In-Memory Fallback Store (for serverless environments without node:sqlite)
const memoryStore = {
  users: new Map(),
  sessions: new Map(),
  history: new Map(), // userId -> Map(animeId -> entry)
  watchlist: new Map(), // userId -> Map(animeId -> entry)
  settings: new Map(),
  nextUserId: 1
};

/**
 * Seed default demo user
 */
function seedDemoUser() {
  const { hash, salt } = hashPassword('demo123');
  const now = Date.now();
  const demoAvatar = 'https://lh3.googleusercontent.com/aida-public/AB6AXuDTN9AsbPHoQqZimkdf5SKxBvZavvxOhDlT8E9vCHRzGbpe1hvxThzKYFlNmpGAueS_ucTqnCwn48iM-L9lqJQFrldtyx9Xp6-ZCo3-27ltcXs134z1k-7sAYvECqoB3KAEdg54dq4IeYszVKZ2P-tUZLVcDT7_3WqhYcw-qmMiNH1qu3LL-qQ12-c0cyUrSwMX7Zl5uNLBTk7kzuIME_SzDxIEadw5iosTQIcUEh-p';

  if (!useMemoryFallback && db) {
    try {
      const countRow = db.prepare('SELECT COUNT(*) as count FROM users').get();
      if (countRow.count === 0) {
        const result = db.prepare(`
          INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `).run('Alex Mercer', 'alex@omcli.io', hash, salt, demoAvatar, 'Community Otaku', 42, now);

        const demoUserId = Number(result.lastInsertRowid);
        db.prepare(`
          INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
          VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
        `).run(demoUserId, now);

        // Seed initial history
        db.prepare(`
          INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
          VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 1, '22676', 740, 1420, ?)
        `).run(demoUserId, now - 3600000);

        // Seed initial watchlist
        db.prepare(`
          INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
          VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 'TV', 7.9, 'Watching', ?)
        `).run(demoUserId, now - 7200000);
      }
    } catch (e) {
      console.warn('[DB] Seed error in SQLite:', e.message);
    }
  }

  // Always seed memoryStore as well
  const demoUser = {
    id: 1,
    username: 'Alex Mercer',
    email: 'alex@omcli.io',
    password_hash: hash,
    salt,
    avatar: demoAvatar,
    tier: 'Community Otaku',
    level: 42,
    created_at: now
  };
  memoryStore.users.set(1, demoUser);
  memoryStore.settings.set(1, {
    user_id: 1,
    preferred_audio: 'sub',
    auto_next: 1,
    auto_skip_intro: 1,
    auto_play: 1,
    preferred_quality: 'auto',
    updated_at: now
  });
  const histMap = new Map();
  histMap.set('20', {
    id: 1,
    user_id: 1,
    anime_id: '20',
    anime_title: 'Naruto',
    poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg',
    episode_num: 1,
    episode_id: '22676',
    progress_seconds: 740,
    duration_seconds: 1420,
    updated_at: now - 3600000
  });
  memoryStore.history.set(1, histMap);
  const watchMap = new Map();
  watchMap.set('20', {
    id: 1,
    user_id: 1,
    anime_id: '20',
    anime_title: 'Naruto',
    poster: 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg',
    format: 'TV',
    score: 7.9,
    status: 'Watching',
    updated_at: now - 7200000
  });
  memoryStore.watchlist.set(1, watchMap);
  memoryStore.nextUserId = 2;
}

seedDemoUser();

/**
 * Register a new user
 */
function registerUser(username, email, password) {
  if (!useMemoryFallback && db) {
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

  // Memory fallback
  for (const u of memoryStore.users.values()) {
    if (u.username === username || u.email === email) {
      throw new Error('Username or email already exists');
    }
  }

  const { hash, salt } = hashPassword(password);
  const now = Date.now();
  const userId = memoryStore.nextUserId++;
  const user = {
    id: userId,
    username,
    email,
    password_hash: hash,
    salt,
    avatar: `https://api.dicebear.com/7.x/bottts-neutral/svg?seed=${encodeURIComponent(username)}`,
    tier: 'Community Otaku',
    level: 1,
    created_at: now
  };
  memoryStore.users.set(userId, user);
  memoryStore.settings.set(userId, {
    user_id: userId,
    preferred_audio: 'sub',
    auto_next: 1,
    auto_skip_intro: 1,
    auto_play: 1,
    preferred_quality: 'auto',
    updated_at: now
  });
  return getUserById(userId);
}

/**
 * Verify login credentials
 */
function verifyUser(usernameOrEmail, password) {
  if (!useMemoryFallback && db) {
    const user = db.prepare('SELECT * FROM users WHERE username = ? OR email = ?').get(usernameOrEmail, usernameOrEmail);
    if (!user) return null;

    const { hash } = hashPassword(password, user.salt);
    if (hash !== user.password_hash) return null;

    return getUserById(user.id);
  }

  // Memory fallback
  for (const user of memoryStore.users.values()) {
    if (user.username === usernameOrEmail || user.email === usernameOrEmail) {
      const { hash } = hashPassword(password, user.salt);
      if (hash === user.password_hash) {
        return getUserById(user.id);
      }
    }
  }
  return null;
}

/**
 * Get user sanitized by ID
 */
function getUserById(id) {
  if (!useMemoryFallback && db) {
    const user = db.prepare('SELECT id, username, email, avatar, tier, level, created_at FROM users WHERE id = ?').get(id);
    return user || null;
  }

  // Memory fallback
  const user = memoryStore.users.get(Number(id));
  if (!user) return null;
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    avatar: user.avatar,
    tier: user.tier || 'Community Otaku',
    level: user.level || 42,
    created_at: user.created_at
  };
}

/**
 * Create session token (valid 30 days)
 */
function createSession(userId) {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000);

  if (!useMemoryFallback && db) {
    db.prepare('INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)').run(token, userId, expiresAt);
  } else {
    memoryStore.sessions.set(token, { token, user_id: Number(userId), expires_at: expiresAt });
  }
  return token;
}

/**
 * Get user by session token
 */
function getUserByToken(token) {
  if (!token) return null;
  if (!useMemoryFallback && db) {
    const session = db.prepare('SELECT * FROM sessions WHERE token = ? AND expires_at > ?').get(token, Date.now());
    if (!session) return null;
    return getUserById(session.user_id);
  }

  // Memory fallback
  const session = memoryStore.sessions.get(token);
  if (!session || session.expires_at <= Date.now()) return null;
  return getUserById(session.user_id);
}

/**
 * Invalidate session token
 */
function removeSession(token) {
  if (!token) return;
  if (!useMemoryFallback && db) {
    db.prepare('DELETE FROM sessions WHERE token = ?').run(token);
  } else {
    memoryStore.sessions.delete(token);
  }
}

/**
 * History methods
 */
function saveWatchHistory(userId, data) {
  const now = Date.now();
  if (!useMemoryFallback && db) {
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
    return;
  }

  // Memory fallback
  const uId = Number(userId);
  if (!memoryStore.history.has(uId)) memoryStore.history.set(uId, new Map());
  const userHist = memoryStore.history.get(uId);
  userHist.set(String(data.animeId), {
    id: Date.now(),
    user_id: uId,
    anime_id: String(data.animeId),
    anime_title: data.animeTitle || 'Anime',
    poster: data.poster || '',
    episode_num: Number(data.episodeNum) || 1,
    episode_id: String(data.episodeId || ''),
    progress_seconds: Number(data.progressSeconds) || 0,
    duration_seconds: Number(data.durationSeconds) || 0,
    updated_at: now
  });
}

function getWatchHistory(userId, limit = 50) {
  if (!useMemoryFallback && db) {
    return db.prepare(`
      SELECT * FROM watch_history 
      WHERE user_id = ? 
      ORDER BY updated_at DESC 
      LIMIT ?
    `).all(userId, limit);
  }

  // Memory fallback
  const userHist = memoryStore.history.get(Number(userId));
  if (!userHist) return [];
  return Array.from(userHist.values())
    .sort((a, b) => b.updated_at - a.updated_at)
    .slice(0, limit);
}

function deleteHistoryItem(userId, animeId) {
  if (!useMemoryFallback && db) {
    db.prepare('DELETE FROM watch_history WHERE user_id = ? AND anime_id = ?').run(userId, String(animeId));
  } else {
    const userHist = memoryStore.history.get(Number(userId));
    if (userHist) userHist.delete(String(animeId));
  }
}

function clearWatchHistory(userId) {
  if (!useMemoryFallback && db) {
    db.prepare('DELETE FROM watch_history WHERE user_id = ?').run(userId);
  } else {
    memoryStore.history.delete(Number(userId));
  }
}

/**
 * Watchlist methods
 */
function addToWatchlist(userId, data) {
  const now = Date.now();
  if (!useMemoryFallback && db) {
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
    return;
  }

  // Memory fallback
  const uId = Number(userId);
  if (!memoryStore.watchlist.has(uId)) memoryStore.watchlist.set(uId, new Map());
  const userWatch = memoryStore.watchlist.get(uId);
  userWatch.set(String(data.animeId), {
    id: Date.now(),
    user_id: uId,
    anime_id: String(data.animeId),
    anime_title: data.animeTitle || 'Anime',
    poster: data.poster || '',
    format: data.format || 'TV',
    score: Number(data.score) || 0,
    status: data.status || 'Watching',
    updated_at: now
  });
}

function getWatchlist(userId, statusFilter = null) {
  if (!useMemoryFallback && db) {
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

  // Memory fallback
  const userWatch = memoryStore.watchlist.get(Number(userId));
  if (!userWatch) return [];
  let list = Array.from(userWatch.values());
  if (statusFilter && statusFilter !== 'All') {
    list = list.filter(item => item.status === statusFilter);
  }
  return list.sort((a, b) => b.updated_at - a.updated_at);
}

function removeFromWatchlist(userId, animeId) {
  if (!useMemoryFallback && db) {
    db.prepare('DELETE FROM watchlist WHERE user_id = ? AND anime_id = ?').run(userId, String(animeId));
  } else {
    const userWatch = memoryStore.watchlist.get(Number(userId));
    if (userWatch) userWatch.delete(String(animeId));
  }
}

function isWatchlisted(userId, animeId) {
  if (!useMemoryFallback && db) {
    const row = db.prepare('SELECT id, status FROM watchlist WHERE user_id = ? AND anime_id = ?').get(userId, String(animeId));
    return row || null;
  }

  const userWatch = memoryStore.watchlist.get(Number(userId));
  if (!userWatch) return null;
  const item = userWatch.get(String(animeId));
  return item ? { id: item.id, status: item.status } : null;
}

/**
 * User Settings
 */
function getUserSettings(userId) {
  if (!useMemoryFallback && db) {
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

  const uId = Number(userId);
  if (!memoryStore.settings.has(uId)) {
    memoryStore.settings.set(uId, {
      user_id: uId,
      preferred_audio: 'sub',
      auto_next: 1,
      auto_skip_intro: 1,
      auto_play: 1,
      preferred_quality: 'auto',
      updated_at: Date.now()
    });
  }
  return memoryStore.settings.get(uId);
}

function updateUserSettings(userId, data) {
  const now = Date.now();
  if (!useMemoryFallback && db) {
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

  const s = getUserSettings(userId);
  if (data.preferredAudio) s.preferred_audio = data.preferredAudio;
  if (data.autoNext !== undefined) s.auto_next = data.autoNext ? 1 : 0;
  if (data.autoSkipIntro !== undefined) s.auto_skip_intro = data.autoSkipIntro ? 1 : 0;
  if (data.autoPlay !== undefined) s.auto_play = data.autoPlay ? 1 : 0;
  if (data.preferredQuality) s.preferred_quality = data.preferredQuality;
  s.updated_at = now;
  return s;
}

/**
 * Aggregate User Stats
 */
function getUserStats(userId) {
  if (!useMemoryFallback && db) {
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

  const history = getWatchHistory(userId);
  const watchlist = getWatchlist(userId);
  return {
    episodesWatched: history.length,
    watchTimeHours: Math.round(history.length * 0.4),
    completedCount: watchlist.filter(w => w.status === 'Completed').length,
    watchlistCount: watchlist.length
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
