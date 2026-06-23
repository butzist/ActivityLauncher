package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import javax.inject.Inject

class ViewIntentParserImpl @Inject constructor() : ViewIntentParser {
    override fun packageFromIntent(intent: Intent): String? {
        val componentName = componentNameFromIntent(intent)
        return componentName?.packageName
    }

    override fun componentNameFromIntent(intent: Intent): ComponentName? {
        if (!intent.action.equals(Intent.ACTION_VIEW)) {
            return null
        }

        return runCatching {
            val url = intent.dataString.orEmpty()
            val rawComponent = url.removePrefix("https://activitylauncher.net/activity/")
            ComponentName.unflattenFromString(rawComponent)
                ?: throw Exception("Invalid component name")
        }.getOrNull()
    }

    override fun parseShortcutIntent(uri: String): Intent? {
        return try {
            Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        } catch (_: Exception) {
            try {
                Intent.parseUri(uri, 0)
            } catch (_: Exception) {
                null
            }
        }
    }
}
