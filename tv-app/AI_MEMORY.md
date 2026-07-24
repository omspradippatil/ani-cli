# Ani-CLI TV App Memory

- **Provider Architecture**: 
  - Backend switched from Consumet to native AllAnime GraphQL scraping due to Consumet deprecation/failures.
  - Implemented dynamic `aaReq` token generation natively in Kotlin (`CryptoUtils.kt`), scraping the JS bundle on the fly to bypass Cloudflare and AllAnime anti-bot protections.
  - Custom `OkHttpClient` is configured to trust all certificates to bypass SSL chain validation errors in emulators.
  - Native GraphQL models updated to fetch `thumbnail` fields for correct poster images instead of dead hardcoded domains.
  - Mobile testing enabled by adding `android.intent.category.LAUNCHER` to `AndroidManifest.xml`.
