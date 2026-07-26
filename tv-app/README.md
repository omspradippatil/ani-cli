# Ani-CLI TV App

A native Android TV application for streaming anime, using the `ani-cli` logic for fetching dynamic stream decryption keys from AllAnime.

## Architecture

- Uses **Native Kotlin** for all networking and cryptography. No external bash scripts or Termux dependencies are required.
- **Dynamic Key Fetching**: Replicates the `ani-cli` logic to parse the `allanime` Javascript bundle, extract the timestamp, `partB`, and 64-character hex mask, and perform a native XOR byte manipulation to retrieve the AES-GCM streaming key.
- **SSL Validation Bypass**: The network layer natively trusts all certificates to allow playback on older emulators that may reject Let's Encrypt certificates.
- **GraphQL**: The app speaks directly to `api.mkissa.net` (the AllAnime GraphQL endpoint) using native Gson deserialization to parse shows, episodes, and thumbnails.
- **Mobile Friendly**: The `AndroidManifest.xml` includes `android.intent.category.LAUNCHER` for easier debugging and deployment on standard Android phones.
- **Stream Extractors**: Includes native Kotlin extractors (`Mp4UploadExtractor.kt`, `OkRuExtractor.kt`) to parse direct MP4 streaming links from iframes.
- **Resilient Video Player**: Uses `androidx.media3.exoplayer` with software decoder fallback (`setEnableDecoderFallback(true)`) to prevent black screens caused by Android Emulator graphics driver glitches (`EGL_BAD_ATTRIBUTE`).
- **Dynamic HTTP Headers & Interceptors**: Injects provider-specific headers (e.g., `Referer`) into the `DefaultHttpDataSource` natively, and uses custom OkHttp interceptors to force `Range` headers to fix ExoPlayer infinite buffering edge-cases.
- **Load Balancing**: Video extractors run concurrently using Kotlin Coroutines with an 8-second execution window (`withTimeoutOrNull`), preventing slow mirrors from freezing the stream loader.
- **Cinematic TV UI**: Built using Jetpack Compose for TV (`androidx.tv.material3`), featuring a seamless Splash Screen and a Netflix-style immersive Hero Banner that crossfades high-resolution backdrops natively based on D-Pad focus events.
