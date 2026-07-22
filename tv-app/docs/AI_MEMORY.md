# AI_MEMORY.md - AniCli TV Architecture & Memory

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose for TV (Material 3)
- **Architecture:** Clean Architecture + MVVM + Repository Pattern
- **Async:** Kotlin Coroutines & Flow
- **Networking:** Retrofit + OkHttp
- **Database:** Room
- **DI:** Hilt
- **Media Player:** AndroidX Media3 ExoPlayer
- **Image Loading:** Coil
- **Serialization:** Kotlinx Serialization

## Folder Structure (Monolith package-by-feature)
- `core/`: Base classes, DI, Theme, standard UI components.
- `data/`: Local/Remote data sources, Repositories, DAOs.
- `domain/`: UseCases, Models, Interfaces.
- `provider/`: Content Provider implementations.
- `player/`: ExoPlayer wrapper and UI controls.
- `ui/`: Compose TV Screens (Home, Search, Details, Settings).

## Recent Architecture Changes
- Initialized base project structure and Gradle configs in `tv-app`.
- Target SDK: 34, Min SDK: 28.
- Added Domain models (`MediaItem`, `MediaRow`, `MediaDetail`, `Episode`, `StreamSource`).
- Added `MediaProvider` interface for interchangeable content sources.
- Added `MockMediaProvider` implementation for UI testing.
- Added Room Database (`AppDatabase`, `WatchProgressDao`, `FavoriteDao`).
- Created Hilt DI modules (`DataModule`, `ProviderModule`).
- Built Compose UI layer (Theme, `HomeScreen`, `DetailsScreen`).
- Integrated `ExoPlayer` wrapped in a Compose `VideoPlayer` component.
- Added Jetpack Navigation in `MainActivity.kt`.
