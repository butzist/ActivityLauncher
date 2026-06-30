package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import javax.inject.Inject

class ViewIntentParserImpl @Inject constructor(
    private val getActivityIconUseCase: GetActivityIconUseCase,
) : ViewIntentParser {
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

    override fun parseLaunchRequest(intent: Intent): LaunchRequest? {
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
            ?: intent.getStringExtra(ShortcutCreator.LEGACY_INTENT_EXTRA_INTENT)
        val launchIntent = launchIntentStr?.let { parseShortcutIntent(it) }

        val component = launchIntent?.component
            ?: componentNameFromIntent(intent)
            ?: return null

        return LaunchRequest(component, launchIntent?.extras)
    }

    override fun parseShortcutRequest(intent: Intent): ShortcutRequest? {
        val appName = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_NAME) ?: ""
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
            ?: intent.getStringExtra(ShortcutCreator.LEGACY_INTENT_EXTRA_INTENT)
        val launchIntent = launchIntentStr?.let { parseShortcutIntent(it) } ?: return null
        val component = launchIntent.component ?: return null

        val iconBundle = intent.getBundleExtra(ShortcutCreator.INTENT_EXTRA_ICON)
        val icon = iconBundle?.let { IconCompat.createFromBundle(it) }
            ?: getActivityIconUseCase(null, component)

        val launcherPluginStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN)
        val launcherPlugin = launcherPluginStr?.let { ComponentName.unflattenFromString(it) }

        return ShortcutRequest(appName, component, icon, launchIntent.extras, launcherPlugin)
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
                val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
                    ?: intent.getStringExtra(ShortcutCreator.LEGACY_INTENT_EXTRA_INTENT)
                    ?: return null
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
