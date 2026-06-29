package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

interface ViewIntentParser {
    fun packageFromIntent(intent: Intent): String?
    fun componentNameFromIntent(intent: Intent): ComponentName?
    fun parseShortcutIntent(uri: String): Intent?
    fun parseLaunchRequest(intent: Intent): LaunchRequest?
    fun parseShortcutRequest(intent: Intent): ShortcutRequest?
}
