# ani-cli Web | Modern Anime Streaming Platform

A lightweight, ad-free, high-performance web streaming application built on top of AniList metadata, HiAnime streaming scrapers, and native HLS stream proxying.

## 🚀 Key Features

- **Ad-Free Streaming**: Clean video playback with zero popups, redirects, or intrusive ads.
- **AniList Integration**: Rich anime metadata (trending, popular, scores, synopsis, high-res posters, banners).
- **Sub & Dub Audio**: Switch between Japanese Audio (Subtitles) and English Dubbed audio on the fly.
- **Embedded Subtitles**: WebVTT subtitle support with custom styling.
- **CORS & Referer Bypass Proxy**: Built-in backend proxy rewrites `.m3u8` playlists and forwards `.ts` video chunks seamlessly so playback never breaks on modern browsers.
- **Android TV Integration**: Direct download button in the navigation bar to download [`ani-cli-production-signed.apk`](download/tv-apk).
- **Oracle Cloud Ready**: Ultra-lightweight memory footprint (<80MB RAM) designed for Oracle Cloud Free Tier ARM/AMD instances.

---

## 🛠️ Quick Start (Local)

1. **Install dependencies:**
   ```bash
   cd website
   npm install
   ```

2. **Start the server:**
   ```bash
   npm start
   ```

3. **Open in browser:**
   ```
   http://localhost:3000
   ```

---

## ☁️ Deploying to Oracle Cloud Server

### Method A: Docker Compose (Recommended)

1. **Copy the `website` directory to your Oracle VM:**
   ```bash
   scp -r website ubuntu@<YOUR_ORACLE_IP>:~/ani-cli-web
   ```

2. **SSH into your server and run:**
   ```bash
   ssh ubuntu@<YOUR_ORACLE_IP>
   cd ~/ani-cli-web
   docker compose up -d --build
   ```

3. **Open firewall ports on Oracle Linux / Ubuntu:**
   ```bash
   # If using UFW:
   sudo ufw allow 3000/tcp
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp

   # If using Oracle iptables:
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 3000 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo netfilter-persistent save
   ```

### Method B: Systemd / PM2 (Direct Node)

1. **Install Node.js & PM2 on Oracle VM:**
   ```bash
   curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g pm2
   ```

2. **Start the application:**
   ```bash
   cd ~/ani-cli-web
   npm ci --omit=dev
   pm2 start server.js --name "ani-cli-stream"
   pm2 save
   pm2 startup
   ```

---

## 📡 API Endpoints

- `GET /api/trending`: Returns top trending anime from AniList.
- `GET /api/popular`: Returns popular anime this season.
- `GET /api/top-rated`: Returns all-time highest-rated anime.
- `GET /api/search?q=:query`: Searches both AniList & HiAnime.
- `GET /api/anime/:id`: Returns AniList metadata and matched streamable episode list.
- `GET /api/stream/:episodeId?mode=sub|dub`: Extracts `.m3u8` stream and subtitle tracks.
- `GET /api/proxy/m3u8?url=:url&ref=:ref`: Proxies and rewrites M3U8 playlists with CORS headers.
- `GET /api/proxy/ts?url=:url&ref=:ref`: Pipes `.ts` video chunks with required `Referer`.
- `GET /download/tv-apk`: Downloads the signed Android TV `.apk`.
