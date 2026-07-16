package de.szalkowski.activitylauncher.domain.model

enum class LaunchSource {
    /**
     * Launched from the main application UI (Package list, Details).
     */
    PRIMARY,

    /**
     * Launched from a home screen shortcut.
     */
    SHORTCUT,

    /**
     * Launched via the ActivityLauncherProxy (external apps).
     */
    PROXY,

    /**
     * Loaded from the local database (Favorites, Recents, Shortcuts list).
     */
    SAVED,

    /**
     * Launched via a web link or deep link.
     */
    DEEPLINK,
}
