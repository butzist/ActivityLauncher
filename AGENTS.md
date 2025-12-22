# Project Overview
Activity Launcher is an Android application that launches hidden activities and creates shortcuts for installed apps. It is an open-source utility tool.

# Tech Stack
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 16
- **Target SDK**: 36
- **Compile SDK**: 36

## Key Libraries
- **Dependency Injection**: Hilt
- **UI Toolkit**: Android Views (XML Layouts) with ViewBinding
- **Navigation**: Android Jetpack Navigation Component
- **UI Components**: Material Design 3 (Material Components)
- **Compatibility**: AndroidX (AppCompat, Core-KTX, Preference, MultiDex)

# Project Structure
The project follows a standard Android Gradle project structure with a single module `:app`.

## Root Directory
- `app/`: Main application module
- `descriptions/`: Store listing descriptions
- `whatsnew/`: Changelogs/Release notes
- `update-listing.py` & `update-translations.sh`: Maintenance scripts

## App Module (`app/src/main/java/de/szalkowski/activitylauncher/`)
- `ui/`: UI related classes (Fragments, Adapters, etc.)
- `services/`: Background services or logic
- `MainActivity.kt`: The main entry point of the application. Handles navigation hosting.
- `ShortcutActivity.kt`: Handles the launching of shortcuts created by the app.
- `ActivityLauncherApp.kt`: The Application class, annotated with `@HiltAndroidApp`.
- `SettingsActivity.kt`: Manages application settings.

# Build Variants
The project uses `productFlavors` with a "distribution" dimension:
1.  **oss**: For direct distribution (F-Droid, etc.). Pure FOSS.
2.  **playStore**: For Google Play Store distribution. Includes `com.google.android.play:review-ktx` for in-app reviews.

# Development Guidelines
- **View Binding**: Used for interacting with XML layouts.
- **Hilt**: Used for dependency injection. Ensure new components are properly annotated (e.g., `@AndroidEntryPoint`).
- **Navigation**: Uses the Navigation Component. Navigation graph is likely defined in `res/navigation`.
- **Service Implementation**: The interface and the default implementation are co-located in the same file (e.g., `FavoritesService.kt`). This is the default/noop convention in all services.

# Handling non-FOSS features
Features that are not free and open-source (like Google Play Services APIs) should not be part of the `oss` build. To achieve this, the project uses Hilt and different service bindings for different product flavors.

An interface for the feature is defined in the `main` source set (e.g., `InAppReviewService`).
Two implementations of this interface are created:
1.  A real implementation that uses non-FOSS APIs, located in the `playStore` source set (e.g., `InAppReviewServiceImpl`).
2.  A stub or no-op implementation that does nothing, located in the `oss` source set (e.g., `InAppReviewServiceImplStub`).

Hilt's `@Binds` in flavor-specific `Bindings.kt` files are used to provide the correct implementation for each build variant. This ensures that the `oss` version remains fully FOSS.

# Verification
Always check if the project builds after applying changes and ensure all build issues are fixed.

## Relevant Commands
- **Build Debug APK**: `./gradlew app:assembleDebug`
- **Check Lint**: `./gradlew app:lintDebug`
- **Run Unit Tests**: `./gradlew app:testDebugUnitTest`
