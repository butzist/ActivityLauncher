package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import javax.inject.Inject

class ViewIntentParserImpl @Inject constructor() : ViewIntentParser {
    override fun packageFromIntent(intent: Intent): String? {
        if (intent.action != Intent.ACTION_VIEW) {
            return null
        }

        return runCatching {
            val url = intent.dataString.orEmpty()
            if (url.startsWith("https://activitylauncher.net/activity/")) {
                val rawComponent = url.removePrefix("https://activitylauncher.net/activity/")
                ComponentName.unflattenFromString(rawComponent)?.packageName
            } else {
                null
            }
        }.getOrNull()
    }

    override fun componentNameFromIntent(intent: Intent): ComponentName? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                runCatching {
                    val url = intent.dataString.orEmpty()
                    val rawComponent = url.removePrefix("https://activitylauncher.net/activity/")
                    ComponentName.unflattenFromString(rawComponent)
                        ?: throw Exception("Invalid component name")
                }.getOrNull()
            }

            ShortcutCreator.INTENT_LAUNCH_SHORTCUT -> {
                val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return null
                parseShortcutIntent(launchIntentStr)?.component
            }

            else -> null
        }
    }

    override fun parseShortcutIntent(uri: String): Intent? {
        val intent = try {
            Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
        } catch (_: Exception) {
            null
        }

        if (intent != null && intent.component != null) {
            return intent
        }

        return try {
            Intent.parseUri(uri, 0)
        } catch (_: Exception) {
            null
        }
    }
}
