# Ani-cli TV App

This is the Android TV implementation of `ani-cli`. 

## Architecture
The application is built using modern Android development practices:
- **Language:** Kotlin
- **UI:** Jetpack Compose for TV (Material 3)
- **Architecture:** Clean Architecture + MVVM + Repository Pattern
- **Async:** Kotlin Coroutines & Flow
- **Networking:** OkHttp (for fetching data from `api.allanime.day` via GraphQL payload)
- **Database:** Room
- **DI:** Hilt
- **Media Player:** AndroidX Media3 ExoPlayer
- **Image Loading:** Coil
- **Serialization:** JSON Parsing with org.json.JSONObject

## Feature Set
- Home screen displaying trending/recent anime.
- Details screen with episodic list.
- Built-in `ExoPlayer` streaming with custom header injection for allanime sources.

## Important Implementation Details
- Dynamic Stream Fetching: Streaming is handled by `PlayerViewModel` executing a network request for the stream URLs based on `episodeId` and then feeding them into the ExoPlayer.
- Headers are dynamically generated inside the ExoPlayer `DefaultHttpDataSource.Factory` to bypass security measures (injecting `sync` payload as query parameter or headers where applicable).
- `RealMediaProvider` makes GraphQL requests against the allanime backend.
