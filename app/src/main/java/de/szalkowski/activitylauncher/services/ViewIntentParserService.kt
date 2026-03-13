package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Intent
import javax.inject.Inject

interface ViewIntentParserService {
    fun packageFromIntent(
        intent: Intent,
    ): String?

    fun componentNameFromIntent(
        intent: Intent,
    ): ComponentName?
}

class ViewIntentParserServiceImpl @Inject constructor() : ViewIntentParserService {
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
}
