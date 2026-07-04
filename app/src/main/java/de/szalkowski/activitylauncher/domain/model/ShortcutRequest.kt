package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat

data class ShortcutRequest(
    val name: String,
    val intent: Intent,
    val icon: IconCompat,
    val launcherPlugin: ComponentName? = null,
)
