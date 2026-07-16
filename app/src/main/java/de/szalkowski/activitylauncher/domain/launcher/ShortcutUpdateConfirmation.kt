package de.szalkowski.activitylauncher.domain.launcher

interface ShortcutUpdateConfirmation {
    suspend fun confirmUpdate(): Boolean
}
