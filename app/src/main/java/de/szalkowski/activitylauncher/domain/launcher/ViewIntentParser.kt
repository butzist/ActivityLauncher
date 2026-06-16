package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.content.Intent

interface ViewIntentParser {
    fun packageFromIntent(
        intent: Intent,
    ): String?

    fun componentNameFromIntent(
        intent: Intent,
    ): ComponentName?
}
