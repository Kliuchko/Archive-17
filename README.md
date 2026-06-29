# Archive 17

Archive 17 is a native Android application for discovering books with Open Library and managing a local personal library.

Current progress:

- Stage 0 project foundation is in place.
- Domain models and the initial Open Library remote API contracts are in place.
- Room entities, DAO contracts, type converters, and schema export are in place.
- Repository contracts and cache-first data coordination are in place.
- Navigation routes and placeholder screens are in place.
- Search is wired to the repository with debounce, loading, empty, and error states.
- Book details are wired to cached Room data, background refresh, and reading status updates.
- My library reads saved books from Room and filters them by reading status.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM
- Navigation Compose
- Retrofit and OkHttp
- Room
- Coil
- Coroutines, Flow, and StateFlow
- Koin

## Architecture Draft

The codebase is organized around a small clean-architecture style split:

- `domain` contains app models, repository contracts, and use cases.
- `data/remote` contains Open Library API definitions and DTOs.
- `data/local` contains Room database, DAO, and entity definitions.
- `data/repository` coordinates remote and local data sources and owns cache behavior.
- `presentation` contains feature screens, ViewModels, and theme code.
- `navigation` contains app destinations and navigation graph setup.
- `di` contains Koin modules.
- `core` contains shared utilities, result types, and common app infrastructure.

The core domain concepts from the exercise are:

- `Work`: the abstract book concept.
- `Edition`: a specific version of a book, including language metadata.
- `LibraryEntry`: the user's local relationship with a saved book.
- `ReadingStatus`: `Want to read`, `Reading`, or `Finished`.

## API Source

The app uses Open Library:

- `GET /search.json` for book search by title or author.
- `GET /works/{workId}.json` for book details, descriptions, and subjects.
- Open Library Covers API for cover image URLs based on cover identifiers.

## Cache Strategy Draft

- Room is the local source of truth for saved books and cached details.
- Saved books should render immediately from local storage.
- Previously opened details should be available offline from cache.
- When the network is available, cached details should refresh in the background.
- Failed refreshes should keep showing cached content with a cached/offline indicator.
- Cached records will include `lastUpdatedAt`.
- The first version will use a 24-hour freshness policy.

## Build

Requirements:

- Android Studio with a recent Android Gradle Plugin compatible environment.
- JDK 17 or the JDK bundled with Android Studio.

Build from the repository root:

```bash
./gradlew :app:assembleDebug
```

Run tests:

```bash
./gradlew test
```

## Planned Screens

- Search
- Book details
- My library
