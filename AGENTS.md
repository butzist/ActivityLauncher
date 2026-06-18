# Project Overview
Activity Launcher is an Android application that launches hidden activities and creates shortcuts for installed apps. It is an open-source utility tool.

# Tech Stack
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 16
- **Target SDK**: 37
- **Compile SDK**: 37

## Key Libraries
- **Dependency Injection**: Hilt
- **UI Toolkit**: Android Views (XML Layouts) with ViewBinding
- **Navigation**: Android Jetpack Navigation Component
- **UI Components**: Material Design 3 (Material Components)
- **Compatibility**: AndroidX (AppCompat, Core-KTX, Preference, MultiDex)

# Project Structure
The project follows a Domain-Driven Design (DDD) structure.

## Root Directory
- `app/`: Main application module
- `descriptions/`: Store listing descriptions
- `whatsnew/`: Changelogs/Release notes
- `update-listing.py` & `update-translations.sh`: Maintenance scripts

## App Module (`app/src/main/java/de/szalkowski/activitylauncher/`)
- `domain/`: Domain models and repository/infrastructure interfaces.
- `data/`: Data source implementations, repositories, and infrastructure.
- `presentation/`: UI Layer (Fragments, ViewModels, Adapters).
- `app/`: Application class and DI modules.
- `entrypoint/`: Activities, Services, Receivers (App entry points).
- `core/`: Shared utilities and base classes.

# Build Variants
The project uses `productFlavors` with a "distribution" dimension and an "ads" dimension.
Distribution:
1.  **oss**: Pure FOSS.
2.  **playStore**: Includes Play Store specific features like in-app reviews.

Ads:
1.  **noads**: Ad-free version.
2.  **ads**: Version with ads (Playwire).

# Development Guidelines
- **View Binding**: Used for interacting with XML layouts.
- **Hilt**: Used for dependency injection.
- **Navigation**: Uses the Navigation Component. Navigation graph is in `res/navigation`.
- **Naming Conventions**: 
    - Interfaces for data access are named `*Repository`.
    - Specific action performers are named by their role (e.g., `ShortcutCreator`, `ActivitySharer`).
    - Interfaces are defined in `domain`, implementations in `data`.
- **Use Cases**: Complex business logic should be extracted from Repositories and ViewModels into standalone Use Cases (Interactors) in the `domain` layer.

# Handling non-FOSS features
Features that are not free and open-source (like Google Play Services APIs) are abstracted into interfaces in the `domain` layer.
Implementations are provided in flavor-specific source sets and bound using Hilt modules in each flavor.

# Verification
Always check if the project builds after applying changes.
When adding new features or modifying existing ones, you **MUST** add or update relevant unit tests and Android (instrumented) tests where possible to ensure correctness and prevent regressions.

## Relevant Commands
- **Build Debug APK**: `./gradlew app:assembleDebug`
- **Check Lint**: `./gradlew app:lintDebug`
- **Run Unit Tests**: `./gradlew app:testDebugUnitTest`

# Code Style and Formatting
This project uses Spotless for automatic code formatting.
- `spotlessApply` on save or before commit.
