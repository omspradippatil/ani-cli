# Om CLI — Precision Anime Streaming Ecosystem

<p align="center">
  <img src="https://lh3.googleusercontent.com/aida-public/AB6AXuDMTWQ0wwQuABhjs_o9Y3AalHAZE_NPqMR5KFhu664c6tGFhpUCpcRBPmBLwrTT__vT6AMDPmMrRoV4xfGKQNcqz8MKxN8NIPq7prq1q8Pdv3jRiJqgONJxs2IeG-Y8laqw3wCVFlr2Fjah3nmAhInBJ08NXFpFJ58vDhMqIq2YRdC4GuwgLrcFEeZUt72D3-rCPMez6Kvi23Mfe0mZZA5fPmgZT8Rc2dRWgzOXLS3s" alt="Om CLI Banner" width="100%" style="border-radius: 16px; max-height: 400px; object-fit: cover;" />
</p>

<p align="center">
  <strong>Next-Generation, Ad-Free Anime Streaming across Web, Mobile, and Android TV.</strong><br>
  Built with obsidian dark aesthetics, instant dual-audio HLS playback, and real-time synchronized watch history.
</p>

<p align="center">
  <a href="https://github.com/omspradippatil/ani-cli/blob/master/LICENSE"><img src="https://img.shields.io/badge/License-MIT-ff4f73.svg" alt="License"></a>
  <a href="https://github.com/omspradippatil/ani-cli/releases"><img src="https://img.shields.io/badge/Android%20TV-Signed%20APK-00e3fd.svg" alt="Android TV APK"></a>
  <a href="https://github.com/omspradippatil/ani-cli"><img src="https://img.shields.io/badge/Version-2.0.0-emerald.svg" alt="Version"></a>
  <a href="https://razorpay.me/@ompradippatil"><img src="https://img.shields.io/badge/Donate-Razorpay-ff3366.svg" alt="Donate"></a>
  <a href="https://github.com/omspradippatil"><img src="https://img.shields.io/badge/Author-Om%20Pradip%20Patil-blueviolet.svg" alt="Author"></a>
</p>

---

## 🌟 Overview

**Om CLI** is a complete anime streaming suite created and maintained by **Om Pradip Patil**. It bridges the gap between terminal speed and modern streaming interfaces by delivering an ad-free, high-fidelity experience across multiple platforms:

1. **Om CLI Web Application** — Fast obsidian kinetic web streaming client with responsive mobile ergonomics, cinema watch room, Artplayer HLS engine, and safe-area touch navigation.
2. **Om CLI for Android TV (`tv-app`)** — Native Android TV application built in Kotlin with Jetpack Compose for TV, ExoPlayer Media3 hardware-accelerated playback, and D-Pad remote navigation.
3. **Edge & Proxy Backend** — High-performance stream resolver, CORS-bypass M3U8 playlist rewriter, and WebVTT caption sanitizer running on Node.js / Cloudflare D1 Edge Workers.

---

## 📱 Platforms & Features

### 🌐 1. Om CLI Web (`website/`)
- **Zero Ads & Zero Popups**: Clean, pure video delivery without malicious redirects or intrusive banners.
- **Obsidian Kinetic Design**: Tailored dark design system with vibrant neon cyan (`#00e3fd`) and crimson (`#ff4f73`) accents.
- **Mobile Responsive & Safe-Area Aware**:
  - Glassmorphic Mobile Bottom Navigation Bar (`Home`, `Browse`, `Watchlist`, `Account`).
  - Fullscreen search drawer with auto-focus and trending tags.
  - Slide-up filter matrix bottom sheet for 30+ genres and tags.
- **Cinema Watch Room**:
  - Dual audio track switching on the fly (**SUB** Japanese / **DUB** English).
  - WebVTT caption engine with automatic HTML formatting (`<i>`, `<b>`, ASS tag cleanup).
  - Seamless auto-orientation to landscape on mobile fullscreen.
  - Multi-resolution stream quality selector (1080p, 720p, 480p, Auto).
  - +85s Opening Skip and Next/Previous episode shortcuts.
- **Cross-Device Cloud Sync**:
  - Cloudflare D1 SQLite database for user sessions, watchlist bookmarks, and resume time progress sync.

### 📺 2. Om CLI for Android TV (`tv-app/`)
- **100% Native Kotlin**: Built natively with Jetpack Compose for TV (`androidx.tv.material3`) and AndroidX Media3 (`ExoPlayer`).
- **TV Remote Navigation**: Smooth focus management, 1.05x scale elevation, and D-Pad friendly controls.
- **Continue Watching Rail**: 16:9 cards with progress bars and dynamic episode badges.
- **Dual Anime Providers**: Integrated AllAnime GraphQL and GoGoAnime scrapers with AES-GCM and AES-256-CBC decryption.
- **Pre-Compiled & Signed Release APK**: Ready to sideload directly onto any Android TV, Google TV, or Fire TV stick.

---

## 📥 Download Android TV App

The production signed Android TV APK is ready to install:

| Build | File | Compatibility | Download |
|---|---|---|---|
| **Production Signed Release** | `ani-cli-production-signed.apk` | Android 7.0+ (API 24+) | [Download Direct APK](https://github.com/omspradippatil/ani-cli/raw/master/ani-cli-production-signed.apk) |

### Installing on Android TV / Fire TV:
1. Download the APK file on your phone/PC, or directly on your TV using the **Downloader** app.
2. Sideload the APK using your preferred file manager (or `adb install ani-cli-production-signed.apk`).
3. Open **Om CLI TV** from your TV apps shelf and start watching instantly.

---

## 🚀 Quick Start (Web Application)

### Prerequisites
- Node.js 20.x or higher
- npm 10.x or higher

### Local Development
```bash
# 1. Clone the repository
git clone https://github.com/omspradippatil/ani-cli.git
cd ani-cli/website

# 2. Install dependencies
npm install

# 3. Start local development server
npm start
```
Open [http://localhost:3000](http://localhost:3000) in your web browser.

### Deploying to Cloudflare Workers (Edge Deployment)
```bash
cd website
# Authenticate with Wrangler
npx wrangler login

# Apply D1 migrations (Local & Remote)
npm run d1:migrate:remote

# Deploy to Cloudflare Workers Edge
npm run deploy:cf
```

---

## 🛠️ Building the TV App from Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 34

```bash
# Navigate to tv-app directory
cd tv-app

# Build release signed APK
./gradlew assembleRelease
```
The compiled signed APK will be output to:  
`tv-app/app/build/outputs/apk/release/app-release.apk`

---

## 💖 Support & Donate

If you enjoy using **Om CLI** and would like to support ongoing development, server maintenance, and edge infrastructure costs, contributions are deeply appreciated!

<p align="center">
  <a href="https://razorpay.me/@ompradippatil" target="_blank">
    <img src="https://img.shields.io/badge/Donate%20via-Razorpay-02042b?style=for-the-badge&logo=razorpay&logoColor=00e3fd" alt="Donate via Razorpay" height="48">
  </a>
</p>

- 💳 **Razorpay (UPI / Credit Card / Debit Card / Netbanking)**: [https://razorpay.me/@ompradippatil](https://razorpay.me/@ompradippatil)
- ⭐ **Star on GitHub**: Star the project on [GitHub](https://github.com/omspradippatil/ani-cli) to help more people discover it!

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the Project (`https://github.com/omspradippatil/ani-cli/fork`)
2. Create your Feature Branch (`git checkout -b feat/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feat/AmazingFeature`)
5. Open a Pull Request

---

## ⚖️ Copyright & License

### Ownership Notice
The Om CLI website, streaming web applications, and Android TV application code, design, and branding are designed, created, and copyrighted by **Om Pradip Patil**.

```
Copyright (c) 2026 Om Pradip Patil <https://github.com/omspradippatil>
All rights reserved.
```

Distributed under the **MIT License**. You are free to inspect, fork, modify, and improve the codebase, provided that the original copyright notice and credit to **Om Pradip Patil** are preserved in all copies or substantial portions of the Software. See [`LICENSE`](LICENSE) for complete details.

### Disclaimer
*Om CLI does not host or store any media files on its servers. All anime metadata, episode names, and streaming sources are aggregated from third-party publicly accessible indexes (such as AniList and community mirrors). All trademarks and copyrights belong to their respective owners.*
