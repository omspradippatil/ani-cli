# Ani-CLI TV App Memory

- **Provider Architecture**: 
  - Backend switched from Consumet to native AllAnime GraphQL scraping due to Consumet deprecation/failures.
  - Implemented dynamic `aaReq` token generation natively in Kotlin (`CryptoUtils.kt`), scraping the JS bundle on the fly to bypass Cloudflare and AllAnime anti-bot protections.
  - Custom `OkHttpClient` is configured to trust all certificates to bypass SSL chain validation errors in emulators.
  - Native GraphQL models updated to fetch `thumbnail` fields for correct poster images instead of dead hardcoded domains.
  - Mobile testing enabled by adding `android.intent.category.LAUNCHER` to `AndroidManifest.xml`.
  - Added specialized stream extractors (`Mp4UploadExtractor.kt` and `OkRuExtractor.kt`) to parse direct video URLs from iframe embeds.
  - Implemented dynamic HTTP header injection (e.g. `Referer` switching for Mp4Upload).
  - Enhanced `VideoPlayer.kt` (ExoPlayer) with `setEnableDecoderFallback(true)` to gracefully handle Android Emulator hardware decoding glitches (`EGL_BAD_ATTRIBUTE`), ensuring streams play rather than displaying a black screen.
  - Added custom OkHttp Interceptor in `DataModule.kt` to inject `Range: bytes=0-` for ExoPlayer requests, explicitly fixing an infinite buffering bug caused by Mp4Upload omitting `Accept-Ranges: bytes`.
  - Implemented Load Balancing/Timeout in `RealMediaProvider.kt` using `withTimeoutOrNull(8000)` to ensure slow or hanging mirrors do not prevent the video player from loading fast streams.
  - Production TV UI overhaul: Added a `SplashScreen.kt` for seamless loading, and updated `HomeScreen.kt` to use an immersive Hero Banner crossfading system (Netflix style) for focused media cards, moving away from simple static grids.
  - **TV Only Enforcement**: Removed all `Mobile` UI components and dual-routing logic to focus exclusively on a robust Android TV experience. 
  - **HLS MimeType Fix**: Explicitly forcing `MimeTypes.APPLICATION_M3U8` for non-MP4 URLs in `VideoPlayer.kt` to prevent ExoPlayer from infinitely buffering while incorrectly probing HLS streams as progressive MP4s.
  - **Continue Watching State**: Fixed `VideoPlayer.kt` missing progress callbacks to properly populate the `HomeViewModel`'s Continue Watching rows via `WatchProgressDao`.
  - **TV Settings UI**: Rebuilt `SettingsScreen.kt` from a single-column layout into a side-by-side Auth vs Settings view properly utilizing D-Pad focused TV Material 3 components instead of raw Android Compose TextFields.
  - **AllAnime Boot Token Fix (2026-07-28)**: AllAnime updated their `x-aa-boot` header algorithm. Old code used a hardcoded HMAC key `"hZ4k0n7u9p2v8q5r"`. New algorithm (reverse-engineered from `DnMm_so9.js` function `MS()`):
    1. `h = HMAC_SHA256(key=maskBytes, msg="aa-boot:t:"+buildId)`
    2. `payload = buildId+":"+keyGroup+":"+hostname+":"+epoch+":"+contentLane`
    3. `token = hex(HMAC_SHA256(key=h, msg=payload))`
    - Where `keyGroup="mkissa"` for mkissa.to (resolved by JS `qS()` function), `hostname="mkissa.to"`, `contentLane="k2"`.
    - The fix is in `generateAaBoot()` in `CryptoUtils.kt`.
    - **Default provider changed to GoGoAnime** as reliable fallback — AllAnime is secondary.


- **GoGoAnime Provider (NEW)**:
  - Added `GoGoAnimeProvider.kt` (`provider/` package) — HTML scraper implementing `MediaProvider`.
    - Scrapes search (`/search.html?keyword=`), detail pages (`/category/{slug}`), and episode pages (`/{slug}-episode-{n}`).
    - Domain is configurable via `SharedPreferences` key `pref_gogoanime_domain` (default: `gogoanime.by`).
    - Falls back through a hardcoded list: `gogoanime.by → gogoanime3.co → anitaku.pe → gogoanime.gg`.
  - Added `GoGoAnimeExtractor.kt` (`extractor/` package) — AES-256-CBC GoGoCDN stream extractor.
    - Matches ani-cli's GoGoAnime decryption flow exactly.
    - Well-known static keys: `KEY=37911490979715163134003223491201`, `IV=3134003223491201`, `KEY2=54674138327930866480207815084989`.
    - Fetches embed page → extracts `data-value` → decrypts → POSTs to `/encrypt-ajax.php` → decrypts response → returns `(quality, m3u8_url)` pairs.
  - Added `ProviderSelector.kt` (`domain/provider/` package) — Hilt singleton that reads `pref_active_provider` from SharedPreferences and routes calls to either `RealMediaProvider` (AllAnime) or `GoGoAnimeProvider`.
  - **All ViewModels** (`HomeViewModel`, `SearchViewModel`, `DetailsViewModel`, `PlayerViewModel`) now inject `ProviderSelector` instead of a hardcoded `MediaProvider`, so the active provider is always resolved at runtime.
  - **ProviderModule.kt** converted from abstract `@Binds` to `@Provides object`, resolving the active `MediaProvider` via `ProviderSelector`.
  - **SettingsScreen.kt** — new Provider toggle buttons (AllAnime / GoGoAnime) in the left Settings column. When GoGoAnime is selected, a "GoGoAnime Mirror Domain" text field appears, letting the user type any working mirror (e.g., `gogoanime3.co`) without a code update.
  - **SettingsViewModel.kt** — added `activeProvider` and `gogoDomain` StateFlows with save methods wired to SharedPreferences.
  - Episode ID format for GoGoAnime: `"{slug}|{episode_number}"` (e.g., `"one-piece-10040|1"`).


- **Web Cloud Backend Provider (NEW & PRIMARY)**:
  - Added `WebApiMediaProvider.kt` (`provider/` package) implementing `MediaProvider`.
    - Directly queries the hosted high-speed Vercel/Node backend (`https://ani-cli-steel.vercel.app`).
    - Eliminates fragile client-side scraping, Cloudflare bot-checks, and reverse-engineering of dynamic HMAC keys.
    - `getHomeFeed()`: Concurrently requests `/api/trending`, `/api/popular`, and `/api/top-rated` for rich AniList metadata and Netflix-style hero banners.
    - `search()`: Instant search via `/api/search?q={query}`.
    - `getDetails()`: Fetches full anime metadata, episode lists, and cleaned descriptions via `/api/anime/{id}`.
    - `getStreamSources()`: Resolves direct high-speed HLS CDN streams (`rawM3u8` with `Referer` headers) with automatic fallback to Web Proxy (`proxyM3u8`).
    - Subtitle integration: Direct VTT subtitle tracks passed into ExoPlayer `SubtitleConfiguration`.
  - Added `WebAnimeService.kt` and `WebDtos.kt` (`data/remote/` package) with Retrofit interface and GSON models.
  - Added `provideWebAnimeService` in `DataModule.kt`.
  - Updated `ProviderSelector.kt`: `PROVIDER_WEB_API = "web_api"` is now the primary default provider across the application.
  - Updated `SettingsScreen.kt` & `SettingsViewModel.kt`: Added Web Cloud toggle and editable Backend URL text field.
  - Added `BackHandler` on remote keyevent across Settings, Details, and Search screens for smooth TV navigation.
  - Verified live on Android Emulator: APK successfully installed, launched, queried backend, loaded home catalog, and streamed episodes with hardware-accelerated HLS playback.
  - **TV Player UI & Cinema Overhaul (2026-10-25)**:
    - Replaced basic Android UI with Obsidian glassmorphic KuroNami cinema player (`VideoPlayer.kt`).
    - Top bar: Precision Stream badge, Server name (e.g. ZokoAnime SUB), Anime Title, Episode Subtitle, 1080p HD tag, and "Audio / Source" stream switcher modal.
    - Center controls: 80dp glowing Crimson Play/Pause button with CyanSoft ring, 10s Rewind, 10s Fast Forward.
    - Bottom bar: Multi-track gradient scrubber (elapsed / buffer / total), "Skip Intro (+85s)" button, "Next Episode" button, and quick Audio Mute/Unmute toggle.
  - **Thumbnail Loading Fix**:
    - Replaced blocking 500ms `rateLimitInterceptor` on the main `OkHttpClient` which was causing Coil image requests to time out and cancel (`IOException: Canceled`).
    - Configured `TvApplication.kt` with Coil `ImageLoaderFactory` injecting custom `OkHttpClient` with SSL trust-all and browser headers (`Mozilla/5.0...`).
    - Implemented image fallback chains in `MediaCard` and `EpisodeCard` (`posterUrl.takeIf { it.isNotBlank() } ?: backdropUrl`).
  - **Audio Volume & Decoding Fix**:
    - Configured `AudioManager.STREAM_MUSIC` volume initialization on startup (boosting emulator default volume from 3 to 12/15).
    - Enabled software decoder fallback in ExoPlayer (`setEnableDecoderFallback(true)` and `EXTENSION_RENDERER_MODE_PREFER`) for AAC/Opus audio tracks.
    - Disabled blocking audio focus muting (`handleAudioFocus = false`) and added explicit `exoPlayer.volume = 1.0f`.
  - **KuroNami Theme (`#0C0E14` Obsidian, `#FF2A5F` Crimson, `#00E3FD` Cyan)**:
    - Updated `Theme.kt`, `HomeScreen.kt`, `SearchScreen.kt`, `DetailsScreen.kt`, and `SettingsScreen.kt` to match the web platform color scheme.
    - Rebuilt `HeroBanner` with multi-stop vignette gradients and gold rating badges.
    - Assembled production release APKs at `ani-cli-production-signed.apk` and `website/public/download/ani-cli-production-signed.apk`.
