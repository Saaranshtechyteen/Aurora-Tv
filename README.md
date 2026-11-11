# Aurora TV: Experience Television, Reimagined.

A Kotlin + Jetpack Compose Android TV launcher with a Liquid Glass look.
Package `com.aurora.tv` | minSdk 28 (Android 9) | targetSdk 34

## Build
1. Open this folder in Android Studio (Koala or newer) and let Gradle sync.
   (No wrapper jar is included. Studio uses `gradle/wrapper/gradle-wrapper.properties`
   to fetch Gradle 8.9; for CLI use run `gradle wrapper` once.)
2. Run on an Android TV emulator / device, or `./gradlew assembleDebug`.
3. Press Home and choose **Aurora TV** as the default launcher.
   Revert via Settings > Apps > Default apps > Home screen.

## Layout
- `MainActivity`            HOME + LEANBACK_LAUNCHER entry, singleTask, package-change receiver (only while visible)
- `AuroraViewModel`         single source of truth; combines installed apps + DataStore settings off the main thread
- `UiState.kt`              pure builder for rows: favourites, recents, continue watching, recommended, etc.
- `data/AppRepository`      PackageManager discovery (leanback first, launcher fallback), launch / info / uninstall
- `data/SettingsStore`      Preferences DataStore: favourites, hidden, recents, order, theme, glass, motion...
- `data/IconCache`          byte-bounded LruCache for icons and TV banners, decoded lazily on IO, trimmed on memory pressure
- `ui/Glass.kt`             Liquid Glass surface (one draw pass, no real-time blur) and `TvFocusable`
- `ui/Wallpapers.kt`        procedural wallpapers + parallax, cross-fade and icon-derived tint
- `ui/*Screen.kt`           Home (hero + rows), grids, Settings, Manage apps, long-press menu

## Controls
D-pad moves focus; OK opens; **hold OK** opens the app menu (open, favourite, reorder, hide, info, uninstall);
Back goes up one level (and never exits the launcher); Home scrolls to top and refocuses Play.
In Settings, Left/Right changes a value.

## Offline content
No network is used. "Continue Watching", "Recommended" and the hero are built from the user's own launch
history, favourites and app categories (video / game / audio). Movies = video apps; TV Shows = Live TV
(TvInputService) apps, falling back to video apps.

## Notes
- Release build is signed with the debug key for convenience. Add your own keystore to publish.
