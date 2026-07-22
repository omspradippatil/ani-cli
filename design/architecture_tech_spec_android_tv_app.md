# Premium Android TV Streaming App - Technical Specification & Architecture

## 1. Project Overview
A high-performance Android TV media streaming application built with Kotlin and Jetpack Compose for TV. Designed for speed (<800ms launch), remote-first navigation, and cinematic user experience.

## 2. Tech Stack
- **UI:** Jetpack Compose for TV (Material 3 TV)
- **Architecture:** MVVM + Clean Architecture + Repository Pattern
- **Threading:** Kotlin Coroutines & Flow
- **Networking:** Retrofit + OkHttp
- **DI:** Hilt
- **Database:** Room (Persistence for History, Favorites, Settings)
- **Media:** AndroidX Media3 ExoPlayer
- **Image Loading:** Coil (optimized for TV downsampling/caching)

## 3. Modular Structure (`app/`)
- `core/`: Common UI components, base classes, DI modules.
- `data/`: Room entities, DAOs, Retrofit interfaces, Repository implementations.
- `domain/`: UseCases and Business Models.
- `ui/`: Compose screens and ViewModels grouped by feature (Home, Search, Details, Player).
- `player/`: Custom ExoPlayer wrapper and TV-specific controls.
- `provider/`: Interface-based content provider system for interchangeability.

## 4. Key Implementation Details

### Focus Management
- Uses `Modifier.focusable()` and `Modifier.onFocusChanged()`.
- Implements `ScalingLazyColumn` and `TvLazyRow` for smooth scrolling.
- Focused elements utilize `animateScaleAsState` for 1.1x scaling and glowing borders.

### Performance Optimization
- **Memory:** Strict <220MB limit during playback using aggressive downsampling in Coil.
- **Lazy Loading:** All lists use `Lazy` components with `remember` and `derivedStateOf`.
- **Pre-fetching:** Media sources are pre-fetched during detail screen navigation.

### Database (Room)
```kotlin
@Entity(tableName = "continue_watching")
data class WatchProgress(
    @PrimaryKey val id: String,
    val mediaTitle: String,
    val progressMs: Long,
    val totalDurationMs: Long,
    val lastUpdated: Long
)
```

### Provider Interface
```kotlin
interface MediaProvider {
    suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>>
    suspend fun search(query: String): Result<List<MediaItem>>
    suspend fun getStreamUrl(mediaId: String): Result<String>
}
```

## 5. Deliverables Included
- **UI Prototypes:** Home, Search, Detail, and Player screens (Material 3 TV).
- **Design System:** Cinematic Dark theme with TV-optimized tokens.
- **Shared Components:** Navigation, focused cards, and transport controls.
