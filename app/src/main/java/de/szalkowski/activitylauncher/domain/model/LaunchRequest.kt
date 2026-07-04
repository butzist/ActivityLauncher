package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent

data class LaunchRequest(
    val intent: Intent,
    val launcherPlugin: ComponentName? = null,
)
