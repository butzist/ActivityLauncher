package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutSource

interface ViewIntentParser {
    fun componentNameFromIntent(intent: Intent): ComponentName?
    fun parseShortcutIntent(uri: String): Intent?
    fun parseLaunchRequest(intent: Intent, source: LaunchSource? = null): LaunchRequest?
    fun parseShortcutRequest(intent: Intent): ShortcutProxyRequest?
    fun parseLaunchSource(intent: Intent): ShortcutSource?
}
