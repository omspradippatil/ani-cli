// website/server.js
// Production-ready Node.js streaming server & API

const express = require('express');
const cors = require('cors');
const path = require('path');

const apiRoutes = require('./routes/api');
const proxyRoutes = require('./routes/proxy');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/user');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging in development
if (process.env.NODE_ENV !== 'production') {
  app.use((req, res, next) => {
    if (!req.url.startsWith('/api/proxy/ts')) {
      console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    }
    next();
  });
}

// Mount Authentication, User & Streaming Proxy routes
app.use('/api/auth', authRoutes);
app.use('/api/user', userRoutes);
app.use('/api', apiRoutes);
app.use('/api/proxy', proxyRoutes);

// Serve static frontend assets
app.use(express.static(path.join(__dirname, 'public')));

// Download route shortcut for TV APK
app.get('/download/tv-apk', (req, res) => {
  const apkPath = path.join(__dirname, 'ani-cli-production-signed.apk');
  res.download(apkPath, 'ani-cli-tv.apk');
});

// Single Page Application (SPA) routing fallback
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Server only if run directly (standalone or Docker, not imported by Vercel serverless)
if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`===============================================`);
    console.log(`🚀 Om CLI Anime Streaming Server is live!`);
    console.log(`📡 Local:   http://localhost:${PORT}`);
    console.log(`🌍 Network: http://0.0.0.0:${PORT}`);
    console.log(`📺 TV APK:  http://localhost:${PORT}/download/tv-apk`);
    console.log(`===============================================`);
  });
}

module.exports = app;
