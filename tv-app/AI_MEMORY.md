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
