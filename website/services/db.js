// website/services/db.js
// Universal Database Service for Om CLI
// Supports: Cloudflare D1 (Native Worker & REST API), Local SQLite (node:sqlite), and In-Memory Fallback

const fs = require('fs');
const path = require('path');
const crypto = require('node:crypto');

// Cloudflare D1 Configuration from environment
const CF_ACCOUNT_ID = process.env.CLOUDFLARE_ACCOUNT_ID;
const CF_DATABASE_ID = process.env.CLOUDFLARE_DATABASE_ID;
const CF_API_TOKEN = process.env.CLOUDFLARE_API_TOKEN;

const isCloudflareD1Api = !!(CF_ACCOUNT_ID && CF_DATABASE_ID && CF_API_TOKEN);

let localSqliteDb = null;
let useMemoryFallback = false;

// Determine writable local DB path
const dbDir = process.env.VERCEL ? '/tmp' : path.join(__dirname, '../data');
const dbPath = path.join(dbDir, 'kuronami.db');

if (isCloudflareD1Api) {
  console.log('[DB] Cloudflare D1 SQLite Mode Active via Cloudflare REST API (Database:', CF_DATABASE_ID.slice(0, 8) + '...)');
} else {
  try {
    if (!fs.existsSync(dbDir)) {
      fs.mkdirSync(dbDir, { recursive: true });
    }

    const { DatabaseSync } = require('node:sqlite');
    localSqliteDb = new DatabaseSync(dbPath);

    // Initialize Local SQLite Tables
    localSqliteDb.exec(`
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

      CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
      CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
      CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);
    `);

    // Sanitize any legacy tier names
    try {
      localSqliteDb.exec(`
        UPDATE users SET tier = 'Community Otaku' WHERE tier LIKE '%VIP%' OR tier IS NULL;
        UPDATE users SET email = 'alex@omcli.io' WHERE email = 'alex@kuronami.io';
      `);
    } catch (e) {}

  } catch (err) {
    console.warn('[DB] Local SQLite DatabaseSync unavailable, initializing memory fallback:', err.message);
    useMemoryFallback = true;
  }
}

/**
 * Execute query against Cloudflare D1 REST API
 */
async function queryCloudflareD1(sql, params = []) {
  const url = `https://api.cloudflare.com/client/v4/accounts/${CF_ACCOUNT_ID}/d1/database/${CF_DATABASE_ID}/query`;
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${CF_API_TOKEN}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ sql, params })
  });

  const json = await res.json();
  if (!json.success) {
    throw new Error(json.errors?.[0]?.message || 'Cloudflare D1 query error');
  }

  const resultObj = json.result?.[0] || {};
  return {
    results: resultObj.results || [],
    meta: resultObj.meta || {}
  };
}

/**
 * Hash password with salt
 */
function hashPassword(password, salt = crypto.randomBytes(16).toString('hex')) {
  const hash = crypto.pbkdf2Sync(password, salt, 100000, 64, 'sha512').toString('hex');
  return { hash, salt };
}

// In-Memory Fallback Store
const memoryStore = {
  users: new Map(),
  sessions: new Map(),
  history: new Map(),
  watchlist: new Map(),
  settings: new Map(),
  nextUserId: 1
};

/**
 * Seed default demo user
 */
async function seedDemoUser() {
  const { hash, salt } = hashPassword('demo123');
  const now = Date.now();
  const demoAvatar = 'https://lh3.googleusercontent.com/aida-public/AB6AXuDTN9AsbPHoQqZimkdf5SKxBvZavvxOhDlT8E9vCHRzGbpe1hvxThzKYFlNmpGAueS_ucTqnCwn48iM-L9lqJQFrldtyx9Xp6-ZCo3-27ltcXs134z1k-7sAYvECqoB3KAEdg54dq4IeYszVKZ2P-tUZLVcDT7_3WqhYcw-qmMiNH1qu3LL-qQ12-c0cyUrSwMX7Zl5uNLBTk7kzuIME_SzDxIEadw5iosTQIcUEh-p';

  if (isCloudflareD1Api) {
    try {
      const check = await queryCloudflareD1('SELECT COUNT(*) as count FROM users');
      const count = check.results?.[0]?.count || 0;
      if (count === 0) {
        await queryCloudflareD1(`
          INSERT INTO users (id, username, email, password_hash, salt, avatar, tier, level, created_at)
          VALUES (1, 'Alex Mercer', 'alex@omcli.io', ?, ?, ?, 'Community Otaku', 42, ?)
        `, [hash, salt, demoAvatar, now]);

        await queryCloudflareD1(`
          INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
          VALUES (1, 'sub', 1, 1, 1, 'auto', ?)
        `, [now]);

        await queryCloudflareD1(`
          INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
          VALUES (1, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 1, '22676', 740, 1420, ?)
        `, [now - 3600000]);

        await queryCloudflareD1(`
          INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
          VALUES (1, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 'TV', 7.9, 'Watching', ?)
        `, [now - 7200000]);
        console.log('[DB] Cloudflare D1 seeded with demo user Alex Mercer');
      }
    } catch (e) {
      console.warn('[DB] Cloudflare D1 seed error:', e.message);
    }
  } else if (!useMemoryFallback && localSqliteDb) {
    try {
      const countRow = localSqliteDb.prepare('SELECT COUNT(*) as count FROM users').get();
      if (countRow.count === 0) {
        const result = localSqliteDb.prepare(`
          INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `).run('Alex Mercer', 'alex@omcli.io', hash, salt, demoAvatar, 'Community Otaku', 42, now);

        const demoUserId = Number(result.lastInsertRowid);
        localSqliteDb.prepare(`
          INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
          VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
        `).run(demoUserId, now);

        localSqliteDb.prepare(`
          INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
          VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 1, '22676', 740, 1420, ?)
        `).run(demoUserId, now - 3600000);

        localSqliteDb.prepare(`
          INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
          VALUES (?, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 'TV', 7.9, 'Watching', ?)
        `).run(demoUserId, now - 7200000);
      }
    } catch (e) {
      console.warn('[DB] SQLite seed error:', e.message);
    }
  }

  // Memory store seed
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
 * Register user
 */
async function registerUser(username, email, password) {
  const { hash, salt } = hashPassword(password);
  const now = Date.now();
  const avatar = `https://api.dicebear.com/7.x/bottts-neutral/svg?seed=${encodeURIComponent(username)}`;

  if (isCloudflareD1Api) {
    const existing = await queryCloudflareD1('SELECT id FROM users WHERE username = ? OR email = ?', [username, email]);
    if (existing.results?.length > 0) throw new Error('Username or email already exists');

    const res = await queryCloudflareD1(`
      INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
      VALUES (?, ?, ?, ?, ?, 'Standard Otaku', 1, ?)
    `, [username, email, hash, salt, avatar, now]);

    const userId = res.meta?.last_row_id;
    await queryCloudflareD1(`
      INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
      VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
    `, [userId, now]);

    return getUserById(userId);
  }

  if (!useMemoryFallback && localSqliteDb) {
    const existing = localSqliteDb.prepare('SELECT id FROM users WHERE username = ? OR email = ?').get(username, email);
    if (existing) throw new Error('Username or email already exists');

    const res = localSqliteDb.prepare(`
      INSERT INTO users (username, email, password_hash, salt, avatar, tier, level, created_at)
      VALUES (?, ?, ?, ?, ?, 'Standard Otaku', 1, ?)
    `).run(username, email, hash, salt, avatar, now);

    const userId = Number(res.lastInsertRowid);
    localSqliteDb.prepare(`
      INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
      VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
    `).run(userId, now);

    return getUserById(userId);
  }

  // Memory fallback
  for (const u of memoryStore.users.values()) {
    if (u.username === username || u.email === email) throw new Error('Username or email already exists');
  }
  const userId = memoryStore.nextUserId++;
  const user = { id: userId, username, email, password_hash: hash, salt, avatar, tier: 'Community Otaku', level: 1, created_at: now };
  memoryStore.users.set(userId, user);
  memoryStore.settings.set(userId, { user_id: userId, preferred_audio: 'sub', auto_next: 1, auto_skip_intro: 1, auto_play: 1, preferred_quality: 'auto', updated_at: now });
  return getUserById(userId);
}

/**
 * Verify user login
 */
async function verifyUser(usernameOrEmail, password) {
  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1('SELECT * FROM users WHERE username = ? OR email = ?', [usernameOrEmail, usernameOrEmail]);
    const user = res.results?.[0];
    if (!user) return null;

    const { hash } = hashPassword(password, user.salt);
    if (hash !== user.password_hash) return null;
    return getUserById(user.id);
  }

  if (!useMemoryFallback && localSqliteDb) {
    const user = localSqliteDb.prepare('SELECT * FROM users WHERE username = ? OR email = ?').get(usernameOrEmail, usernameOrEmail);
    if (!user) return null;

    const { hash } = hashPassword(password, user.salt);
    if (hash !== user.password_hash) return null;
    return getUserById(user.id);
  }

  // Memory fallback
  for (const user of memoryStore.users.values()) {
    if (user.username === usernameOrEmail || user.email === usernameOrEmail) {
      const { hash } = hashPassword(password, user.salt);
      if (hash === user.password_hash) return getUserById(user.id);
    }
  }
  return null;
}

/**
 * Get user by ID
 */
async function getUserById(id) {
  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1('SELECT id, username, email, avatar, tier, level, created_at FROM users WHERE id = ?', [Number(id)]);
    return res.results?.[0] || null;
  }

  if (!useMemoryFallback && localSqliteDb) {
    const user = localSqliteDb.prepare('SELECT id, username, email, avatar, tier, level, created_at FROM users WHERE id = ?').get(Number(id));
    return user || null;
  }

  const user = memoryStore.users.get(Number(id));
  if (!user) return null;
  return { id: user.id, username: user.username, email: user.email, avatar: user.avatar, tier: user.tier || 'Community Otaku', level: user.level || 42, created_at: user.created_at };
}

/**
 * Create session token
 */
async function createSession(userId) {
  const token = crypto.randomBytes(32).toString('hex');
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000);

  if (isCloudflareD1Api) {
    await queryCloudflareD1('INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)', [token, Number(userId), expiresAt]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare('INSERT INTO sessions (token, user_id, expires_at) VALUES (?, ?, ?)').run(token, Number(userId), expiresAt);
  } else {
    memoryStore.sessions.set(token, { token, user_id: Number(userId), expires_at: expiresAt });
  }
  return token;
}

/**
 * Get user by token
 */
async function getUserByToken(token) {
  if (!token) return null;

  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1('SELECT * FROM sessions WHERE token = ? AND expires_at > ?', [token, Date.now()]);
    const session = res.results?.[0];
    if (!session) return null;
    return getUserById(session.user_id);
  }

  if (!useMemoryFallback && localSqliteDb) {
    const session = localSqliteDb.prepare('SELECT * FROM sessions WHERE token = ? AND expires_at > ?').get(token, Date.now());
    if (!session) return null;
    return getUserById(session.user_id);
  }

  const session = memoryStore.sessions.get(token);
  if (!session || session.expires_at <= Date.now()) return null;
  return getUserById(session.user_id);
}

/**
 * Remove session
 */
async function removeSession(token) {
  if (!token) return;
  if (isCloudflareD1Api) {
    await queryCloudflareD1('DELETE FROM sessions WHERE token = ?', [token]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare('DELETE FROM sessions WHERE token = ?').run(token);
  } else {
    memoryStore.sessions.delete(token);
  }
}

/**
 * Watch History
 */
async function saveWatchHistory(userId, data) {
  const now = Date.now();
  const sql = `
    INSERT INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, anime_id) DO UPDATE SET
      episode_num = excluded.episode_num,
      episode_id = excluded.episode_id,
      progress_seconds = excluded.progress_seconds,
      duration_seconds = excluded.duration_seconds,
      updated_at = excluded.updated_at
  `;
  const params = [
    Number(userId),
    String(data.animeId),
    data.animeTitle || 'Anime',
    data.poster || '',
    Number(data.episodeNum) || 1,
    String(data.episodeId || ''),
    Number(data.progressSeconds) || 0,
    Number(data.durationSeconds) || 0,
    now
  ];

  if (isCloudflareD1Api) {
    await queryCloudflareD1(sql, params);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare(sql).run(...params);
  } else {
    const uId = Number(userId);
    if (!memoryStore.history.has(uId)) memoryStore.history.set(uId, new Map());
    memoryStore.history.get(uId).set(String(data.animeId), {
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
}

async function getWatchHistory(userId, limit = 50) {
  const sql = `SELECT * FROM watch_history WHERE user_id = ? ORDER BY updated_at DESC LIMIT ?`;
  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1(sql, [Number(userId), limit]);
    return res.results || [];
  }
  if (!useMemoryFallback && localSqliteDb) {
    return localSqliteDb.prepare(sql).all(Number(userId), limit);
  }
  const userHist = memoryStore.history.get(Number(userId));
  if (!userHist) return [];
  return Array.from(userHist.values()).sort((a, b) => b.updated_at - a.updated_at).slice(0, limit);
}

async function deleteHistoryItem(userId, animeId) {
  if (isCloudflareD1Api) {
    await queryCloudflareD1('DELETE FROM watch_history WHERE user_id = ? AND anime_id = ?', [Number(userId), String(animeId)]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare('DELETE FROM watch_history WHERE user_id = ? AND anime_id = ?').run(Number(userId), String(animeId));
  } else {
    const userHist = memoryStore.history.get(Number(userId));
    if (userHist) userHist.delete(String(animeId));
  }
}

async function clearWatchHistory(userId) {
  if (isCloudflareD1Api) {
    await queryCloudflareD1('DELETE FROM watch_history WHERE user_id = ?', [Number(userId)]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare('DELETE FROM watch_history WHERE user_id = ?').run(Number(userId));
  } else {
    memoryStore.history.delete(Number(userId));
  }
}

async function deleteAccount(userId) {
  const uid = Number(userId);
  if (isCloudflareD1Api) {
    await queryCloudflareD1('DELETE FROM sessions WHERE user_id = ?', [uid]);
    await queryCloudflareD1('DELETE FROM watch_history WHERE user_id = ?', [uid]);
    await queryCloudflareD1('DELETE FROM watchlist WHERE user_id = ?', [uid]);
    await queryCloudflareD1('DELETE FROM user_settings WHERE user_id = ?', [uid]);
    await queryCloudflareD1('DELETE FROM users WHERE id = ?', [uid]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.exec('BEGIN TRANSACTION;');
    try {
      localSqliteDb.prepare('DELETE FROM sessions WHERE user_id = ?').run(uid);
      localSqliteDb.prepare('DELETE FROM watch_history WHERE user_id = ?').run(uid);
      localSqliteDb.prepare('DELETE FROM watchlist WHERE user_id = ?').run(uid);
      localSqliteDb.prepare('DELETE FROM user_settings WHERE user_id = ?').run(uid);
      localSqliteDb.prepare('DELETE FROM users WHERE id = ?').run(uid);
      localSqliteDb.exec('COMMIT;');
    } catch (err) {
      localSqliteDb.exec('ROLLBACK;');
      throw err;
    }
  } else {
    for (const [token, sess] of memoryStore.sessions.entries()) {
      if (sess.user_id === uid) memoryStore.sessions.delete(token);
    }
    memoryStore.history.delete(uid);
    memoryStore.watchlist.delete(uid);
    memoryStore.settings.delete(uid);
    for (const [id, u] of memoryStore.users.entries()) {
      if (u.id === uid) memoryStore.users.delete(id);
    }
  }
}

/**
 * Watchlist
 */
async function addToWatchlist(userId, data) {
  const now = Date.now();
  const sql = `
    INSERT INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, anime_id) DO UPDATE SET
      status = excluded.status,
      updated_at = excluded.updated_at
  `;
  const params = [
    Number(userId),
    String(data.animeId),
    data.animeTitle || 'Anime',
    data.poster || '',
    data.format || 'TV',
    Number(data.score) || 0,
    data.status || 'Watching',
    now
  ];

  if (isCloudflareD1Api) {
    await queryCloudflareD1(sql, params);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare(sql).run(...params);
  } else {
    const uId = Number(userId);
    if (!memoryStore.watchlist.has(uId)) memoryStore.watchlist.set(uId, new Map());
    memoryStore.watchlist.get(uId).set(String(data.animeId), {
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
}

async function getWatchlist(userId, statusFilter = null) {
  let sql = 'SELECT * FROM watchlist WHERE user_id = ?';
  const params = [Number(userId)];
  if (statusFilter && statusFilter !== 'All') {
    sql += ' AND status = ?';
    params.push(statusFilter);
  }
  sql += ' ORDER BY updated_at DESC';

  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1(sql, params);
    return res.results || [];
  }
  if (!useMemoryFallback && localSqliteDb) {
    return localSqliteDb.prepare(sql).all(...params);
  }
  const userWatch = memoryStore.watchlist.get(Number(userId));
  if (!userWatch) return [];
  let list = Array.from(userWatch.values());
  if (statusFilter && statusFilter !== 'All') list = list.filter(item => item.status === statusFilter);
  return list.sort((a, b) => b.updated_at - a.updated_at);
}

async function removeFromWatchlist(userId, animeId) {
  if (isCloudflareD1Api) {
    await queryCloudflareD1('DELETE FROM watchlist WHERE user_id = ? AND anime_id = ?', [Number(userId), String(animeId)]);
  } else if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare('DELETE FROM watchlist WHERE user_id = ? AND anime_id = ?').run(Number(userId), String(animeId));
  } else {
    const userWatch = memoryStore.watchlist.get(Number(userId));
    if (userWatch) userWatch.delete(String(animeId));
  }
}

async function isWatchlisted(userId, animeId) {
  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1('SELECT id, status FROM watchlist WHERE user_id = ? AND anime_id = ?', [Number(userId), String(animeId)]);
    return res.results?.[0] || null;
  }
  if (!useMemoryFallback && localSqliteDb) {
    return localSqliteDb.prepare('SELECT id, status FROM watchlist WHERE user_id = ? AND anime_id = ?').get(Number(userId), String(animeId)) || null;
  }
  const userWatch = memoryStore.watchlist.get(Number(userId));
  if (!userWatch) return null;
  const item = userWatch.get(String(animeId));
  return item ? { id: item.id, status: item.status } : null;
}

/**
 * User Settings
 */
async function getUserSettings(userId) {
  if (isCloudflareD1Api) {
    const res = await queryCloudflareD1('SELECT * FROM user_settings WHERE user_id = ?', [Number(userId)]);
    let s = res.results?.[0];
    if (!s) {
      const now = Date.now();
      await queryCloudflareD1(`
        INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
        VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
      `, [Number(userId), now]);
      const res2 = await queryCloudflareD1('SELECT * FROM user_settings WHERE user_id = ?', [Number(userId)]);
      s = res2.results?.[0];
    }
    return s;
  }

  if (!useMemoryFallback && localSqliteDb) {
    let settings = localSqliteDb.prepare('SELECT * FROM user_settings WHERE user_id = ?').get(Number(userId));
    if (!settings) {
      const now = Date.now();
      localSqliteDb.prepare(`
        INSERT INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
        VALUES (?, 'sub', 1, 1, 1, 'auto', ?)
      `).run(Number(userId), now);
      settings = localSqliteDb.prepare('SELECT * FROM user_settings WHERE user_id = ?').get(Number(userId));
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

async function updateUserSettings(userId, data) {
  const now = Date.now();
  if (isCloudflareD1Api) {
    await queryCloudflareD1(`
      UPDATE user_settings 
      SET preferred_audio = COALESCE(?, preferred_audio),
          auto_next = COALESCE(?, auto_next),
          auto_skip_intro = COALESCE(?, auto_skip_intro),
          auto_play = COALESCE(?, auto_play),
          preferred_quality = COALESCE(?, preferred_quality),
          updated_at = ?
      WHERE user_id = ?
    `, [
      data.preferredAudio,
      data.autoNext !== undefined ? (data.autoNext ? 1 : 0) : null,
      data.autoSkipIntro !== undefined ? (data.autoSkipIntro ? 1 : 0) : null,
      data.autoPlay !== undefined ? (data.autoPlay ? 1 : 0) : null,
      data.preferredQuality,
      now,
      Number(userId)
    ]);
    return getUserSettings(userId);
  }

  if (!useMemoryFallback && localSqliteDb) {
    localSqliteDb.prepare(`
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
      Number(userId)
    );
    return getUserSettings(userId);
  }

  const s = await getUserSettings(userId);
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
async function getUserStats(userId) {
  if (isCloudflareD1Api) {
    const epRes = await queryCloudflareD1('SELECT COUNT(*) as total_entries, SUM(progress_seconds) as total_seconds FROM watch_history WHERE user_id = ?', [Number(userId)]);
    const compRes = await queryCloudflareD1("SELECT COUNT(*) as count FROM watchlist WHERE user_id = ? AND LOWER(status) = 'completed'", [Number(userId)]);
    const watchRes = await queryCloudflareD1('SELECT COUNT(*) as count, AVG(score) as avgScore FROM watchlist WHERE user_id = ?', [Number(userId)]);

    const totalSeconds = epRes.results?.[0]?.total_seconds || 0;
    const totalHours = Math.round(totalSeconds / 3600);
    const avgScore = watchRes.results?.[0]?.avgScore ? Number(watchRes.results[0].avgScore).toFixed(1) : '--';

    return {
      totalEpisodesWatched: epRes.results?.[0]?.total_entries || 0,
      totalDaysWatched: (totalSeconds / 86400).toFixed(1),
      completedSeriesCount: compRes.results?.[0]?.count || 0,
      watchlistCount: watchRes.results?.[0]?.count || 0,
      meanScore: avgScore,
      episodesWatched: epRes.results?.[0]?.total_entries || 0,
      watchTimeHours: totalHours,
      completedCount: compRes.results?.[0]?.count || 0
    };
  }

  if (!useMemoryFallback && localSqliteDb) {
    const totalEpisodesRow = localSqliteDb.prepare(`
      SELECT COUNT(*) as total_entries, COALESCE(SUM(progress_seconds), 0) as total_seconds
      FROM watch_history WHERE user_id = ?
    `).get(Number(userId));

    const completedCountRow = localSqliteDb.prepare(`
      SELECT COUNT(*) as count FROM watchlist WHERE user_id = ? AND LOWER(status) = 'completed'
    `).get(Number(userId));

    const watchlistCountRow = localSqliteDb.prepare(`
      SELECT COUNT(*) as count, AVG(score) as avgScore FROM watchlist WHERE user_id = ?
    `).get(Number(userId));

    const totalSeconds = totalEpisodesRow?.total_seconds || 0;
    const totalHours = Math.round(totalSeconds / 3600);
    const avgScore = watchlistCountRow?.avgScore ? Number(watchlistCountRow.avgScore).toFixed(1) : '--';

    return {
      totalEpisodesWatched: totalEpisodesRow?.total_entries || 0,
      totalDaysWatched: (totalSeconds / 86400).toFixed(1),
      completedSeriesCount: completedCountRow?.count || 0,
      watchlistCount: watchlistCountRow?.count || 0,
      meanScore: avgScore,
      episodesWatched: totalEpisodesRow?.total_entries || 0,
      watchTimeHours: totalHours,
      completedCount: completedCountRow?.count || 0
    };
  }

  const history = await getWatchHistory(userId);
  const watchlist = await getWatchlist(userId);
  return {
    totalEpisodesWatched: history.length,
    totalDaysWatched: ((history.length * 24 * 60) / 86400).toFixed(1),
    completedSeriesCount: watchlist.filter(w => (w.status || '').toLowerCase() === 'completed').length,
    watchlistCount: watchlist.length,
    meanScore: '--',
    episodesWatched: history.length,
    watchTimeHours: Math.round(history.length * 0.4),
    completedCount: watchlist.filter(w => (w.status || '').toLowerCase() === 'completed').length
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
  getUserStats,
  deleteAccount
};
