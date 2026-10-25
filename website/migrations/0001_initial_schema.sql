-- Migration 0001: Initial Om CLI SQLite & Cloudflare D1 Schema

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

-- Indexes for ultra-fast query performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_sessions_token ON sessions(token);
CREATE INDEX IF NOT EXISTS idx_history_user ON watch_history(user_id);
CREATE INDEX IF NOT EXISTS idx_watchlist_user ON watchlist(user_id);

-- Default Demo Otaku User (Password: demo123)
INSERT OR IGNORE INTO users (id, username, email, password_hash, salt, avatar, tier, level, created_at)
VALUES (
  1,
  'Alex Mercer',
  'alex@omcli.io',
  '807a9b0c2a233634282f1854bb610fec6037130b050d4b9c1d0f5cb70d58aa89f5bc3a6703b41d2e105e1fcbeee2c07ef4662d515a452fcfa09be7fe0f496155',
  '08a1fa923055375437145719b33a5956',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDTN9AsbPHoQqZimkdf5SKxBvZavvxOhDlT8E9vCHRzGbpe1hvxThzKYFlNmpGAueS_ucTqnCwn48iM-L9lqJQFrldtyx9Xp6-ZCo3-27ltcXs134z1k-7sAYvECqoB3KAEdg54dq4IeYszVKZ2P-tUZLVcDT7_3WqhYcw-qmMiNH1qu3LL-qQ12-c0cyUrSwMX7Zl5uNLBTk7kzuIME_SzDxIEadw5iosTQIcUEh-p',
  'Community Otaku',
  42,
  1700000000000
);

INSERT OR IGNORE INTO user_settings (user_id, preferred_audio, auto_next, auto_skip_intro, auto_play, preferred_quality, updated_at)
VALUES (1, 'sub', 1, 1, 1, 'auto', 1700000000000);

-- Initial sample history
INSERT OR IGNORE INTO watch_history (user_id, anime_id, anime_title, poster, episode_num, episode_id, progress_seconds, duration_seconds, updated_at)
VALUES (1, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 1, '22676', 740, 1420, 1700000000000);

-- Initial sample watchlist
INSERT OR IGNORE INTO watchlist (user_id, anime_id, anime_title, poster, format, score, status, updated_at)
VALUES (1, '20', 'Naruto', 'https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20-YJvLbgjjdFYI.jpg', 'TV', 7.9, 'Watching', 1700000000000);
