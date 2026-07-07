package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat

data class LaunchRequest(
    val intent: Intent,
    val name: String? = null,
    val icon: IconCompat? = null,
    val launcherPlugin: ComponentName? = null,
)
