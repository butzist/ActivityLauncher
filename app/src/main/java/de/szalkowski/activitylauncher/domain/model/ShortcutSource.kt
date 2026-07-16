package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent

data class ShortcutSource(
    val id: String?,
    val intent: Intent?,
    val signature: String?,
    val launcherPlugin: ComponentName?,
)

sealed class ShortcutResolutionResult {
    data class Success(val request: LaunchRequest) : ShortcutResolutionResult()
    object NotFound : ShortcutResolutionResult()
    object InvalidSignature : ShortcutResolutionResult()
}
