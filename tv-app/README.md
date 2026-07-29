# Ani-CLI TV App

A native Android TV application for streaming anime, using the `ani-cli` logic for fetching dynamic stream decryption keys from AllAnime — now with **dual-provider support** including GoGoAnime.

## Architecture

- Uses **Native Kotlin** for all networking and cryptography. No external bash scripts or Termux dependencies are required.
- **Dynamic Key Fetching**: Replicates the `ani-cli` logic to parse the `allanime` Javascript bundle, extract the timestamp, `partB`, and 64-character hex mask, and perform a native XOR byte manipulation to retrieve the AES-GCM streaming key.
- **SSL Validation Bypass**: The network layer natively trusts all certificates to allow playback on older emulators that may reject Let's Encrypt certificates.
- **GraphQL**: The app speaks directly to `api.mkissa.net` (the AllAnime GraphQL endpoint) using native Gson deserialization to parse shows, episodes, and thumbnails.
- **TV Only**: The app is strictly TV-focused. All mobile-specific dual-routing components have been permanently deleted.
- **Stream Extractors**: Includes native Kotlin extractors (`Mp4UploadExtractor.kt`, `OkRuExtractor.kt`, `GoGoAnimeExtractor.kt`) to parse direct streaming links from iframes and CDN embeds.
- **Resilient Video Player**: Uses `androidx.media3.exoplayer` with software decoder fallback (`setEnableDecoderFallback(true)`) to prevent black screens caused by Android Emulator graphics driver glitches (`EGL_BAD_ATTRIBUTE`). Explicitly forces HLS (`MimeTypes.APPLICATION_M3U8`) for AllAnime `.m3u` streams missing file extensions, which previously caused infinite buffering.
- **Dynamic HTTP Headers & Interceptors**: Injects provider-specific headers (e.g., `Referer`) into the `DefaultHttpDataSource` natively, and uses custom OkHttp interceptors to force `Range` headers to fix ExoPlayer infinite buffering edge-cases.
- **Load Balancing**: Video extractors run concurrently using Kotlin Coroutines with an 8-second execution window (`withTimeoutOrNull`), preventing slow mirrors from freezing the stream loader.
- **Cinematic TV UI**: Built using Jetpack Compose for TV (`androidx.tv.material3`), featuring a seamless Splash Screen, a Netflix-style immersive Hero Banner that crossfades high-resolution backdrops natively based on D-Pad focus events, and a functional split-screen "Settings & Auth" interface optimized for TV remotes.

## Dual Provider System

The app supports two anime providers, switchable at runtime via the Settings screen:

| Provider | Source | Technology |
|---|---|---|
| **AllAnime** (default) | `api.mkissa.net` GraphQL | Native AES-GCM + aaReq token generation |
| **GoGoAnime** | `gogoanime.by` (configurable) | HTML scraping + GoGoCDN AES-256-CBC decryption |

### Provider Architecture

- **`ProviderSelector`** — Hilt singleton that reads `pref_active_provider` from SharedPreferences. All ViewModels inject this instead of a hardcoded provider, so switching takes effect immediately.
- **`RealMediaProvider`** — AllAnime GraphQL native scraper (original provider).
- **`GoGoAnimeProvider`** — GoGoAnime HTML scraper. Searches, parses detail pages, and extracts streams via `GoGoAnimeExtractor`.
- **`GoGoAnimeExtractor`** — Implements the same AES-256-CBC GoGoCDN decryption as ani-cli:
  - Known static keys: `KEY=37911490979715163134003223491201`, `IV=3134003223491201`
  - Fetches embed → decrypts `data-value` → POSTs to `/encrypt-ajax.php` → decrypts response with `KEY2` → returns `.m3u8` URLs sorted by quality.

### GoGoAnime Mirror Domain (ISP Block Workaround)

GoGoAnime frequently migrates domains to avoid ISP blocks. The Settings screen includes a **"GoGoAnime Mirror Domain"** field that lets you enter any working mirror directly from your TV:

1. Go to **Settings** → select **GoGoAnime** as your source
2. The "GoGoAnime Mirror Domain" field appears
3. Type the current working domain (e.g., `gogoanime3.co`, `anitaku.pe`)
4. Press **Save Domain** — the app switches immediately, no code update needed

Known working mirrors: `gogoanime.by` · `gogoanime3.co` · `anitaku.pe`

## Episode ID Formats

- **AllAnime**: `"{allAnimeId}|{episodeNumber}"` — e.g. `"YmFzZTY0....|1"`
- **GoGoAnime**: `"{slug}|{episodeNumber}"` — e.g. `"one-piece-10040|1"`
