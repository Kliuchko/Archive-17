# Archive 17

Archive 17 is a native Android application for discovering books with Open Library and managing a local personal library.

## Features

- Search books by title or author after at least two characters.
- Debounced Open Library search with loading, empty, and error states.
- Book details with cached data, background refresh, language metadata, subjects, and reading status.
- Personal library stored in Room and available offline.
- Library filtering by reading status: Want to read, Reading, and Finished.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM
- Navigation Compose
- Retrofit, OkHttp, and Gson
- Room
- Coil
- Coroutines, Flow, and StateFlow
- Koin

## Architecture

The codebase uses a small clean-architecture style split:

- `domain` contains app models and repository contracts.
- `data/networking` contains Open Library API definitions, DTOs, and DTO-to-domain mapping.
- `data/local` contains Room database, DAO, entities, relations, type converters, and local mapping.
- `data/repository` coordinates remote and local data sources and owns cache behavior.
- `presentation` contains Compose screens, ViewModels, navigation, and theme code.
- `di` contains Koin modules.
- `core` contains shared infrastructure such as time providers.

Core domain concepts:

- `Work`: the abstract book concept.
- `Edition`: a specific version of a book, including language metadata.
- `LibraryEntry`: the user's locally saved relationship with a work.
- `ReadingStatus`: Want to read, Reading, or Finished.

## API Source

The app uses Open Library:

- `GET /search.json` for book search by title or author.
- `GET /works/{workId}.json` for additional details, descriptions, and subjects.
- Open Library Covers API for cover image URLs based on cover identifiers.

## Cache Strategy

- Room is the local source of truth for saved books and cached details.
- Search results are cached as `Work` records so details can open immediately from local data.
- Details screens observe Room first, then refresh Open Library details in the background.
- If refresh fails and cached data exists, the cached content stays visible with a cached/stale indicator.
- If no cached data exists and refresh fails, the details screen shows an error state.
- Cached records include `lastUpdatedAt`.
- The first version uses a 24-hour freshness policy.

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

## Known Limitations

- The app does not implement an e-book reader, file downloads, authentication, reading progress, bookmarks, or annotations.
- Edition language metadata is based on the search endpoint's language field in this version.
- The UI is intentionally functional and restrained; final visual polish can happen after core behavior review.
