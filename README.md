# AnimeTracker

A simple anime watchlist tracker built with Kotlin, Jetpack Compose, Room, and MVVM.

## What's included

- Add, edit, and delete anime in your watchlist
- Track episodes watched with a one-tap "+ Episode" button
- Optional total-episode count with a progress bar
- **Status**: Watching / Completed / Plan to Watch, settable per anime and
  filterable from the home screen
- **Rating**: tap-to-set 1–10 star rating
- **Search bar**: filters the list by name as you type (combines with the
  status filter)
- Data is stored locally with Room and survives app restarts
- Material 3 UI with automatic light/dark theme + Android 12+ dynamic color
- MVVM architecture (Compose UI → ViewModel → Repository → Room)

## How to open it

1. Unzip this folder somewhere on your computer.
2. Open **Android Studio** → File → Open → select the `AnimeTracker` folder.
3. Let Gradle sync (first sync downloads dependencies, so it needs internet).
4. Click Run ▶ on an emulator or physical device.

**Note on the Gradle wrapper:** this project doesn't include the `gradle-wrapper.jar`
binary (it can't be generated outside of Gradle/Android Studio itself). Opening the
folder in Android Studio is all you need — it will configure Gradle automatically on
first sync. If you ever want to build from the command line, run
`gradle wrapper` once (with Gradle installed) to regenerate the wrapper files.

**Note on the database:** adding status + rating bumped the Room schema from
version 1 to 2. There's no real `Migration` yet, so on that one upgrade Room
just recreates the table (see the comment in `AnimeDatabase.kt`) — meaning if
you'd already installed the earlier version and added anime, that data is
cleared on this update. From here on, add a `Migration` if you want future
schema changes to preserve data.

## Project structure

```
app/src/main/java/com/example/animetracker/
├── MainActivity.kt              # Single activity, hosts the Compose UI
├── data/
│   ├── Anime.kt                 # Room entity (name, episodes, status, rating)
│   ├── AnimeStatus.kt           # Watching / Completed / Plan to Watch enum
│   ├── Converters.kt            # Room TypeConverter for AnimeStatus
│   ├── AnimeDao.kt              # Database queries
│   ├── AnimeDatabase.kt         # Room database + singleton
│   └── AnimeRepository.kt       # Mediates between ViewModel and DAO
├── viewmodel/
│   └── AnimeViewModel.kt        # UI state, search/filter logic, actions
└── ui/
    ├── theme/                   # Color.kt, Theme.kt, Type.kt (Material 3)
    ├── screens/
    │   ├── HomeScreen.kt        # Watchlist screen: search, filters, list, FAB
    │   └── AddEditAnimeDialog.kt
    └── components/
        └── AnimeCard.kt         # One card in the list (status + rating badges)
```

The package name is `com.example.animetracker` and the app ID is the same —
you'll likely want to change both to your own before publishing anywhere; in
Android Studio that's Right-click package → Refactor → Rename, plus updating
`applicationId`/`namespace` in `app/build.gradle.kts`.

## Ideas for what to build next

- **Sorting** — by name, rating, or last updated, via a menu in the top bar.
- **Genres/tags** — a list column on `Anime` plus chips on the card.
- **Cover images** — pick from gallery (Coil for loading, a URI column to store it).
- **Stats screen** — total anime, episodes watched, average rating, via a
  second screen or bottom sheet.
