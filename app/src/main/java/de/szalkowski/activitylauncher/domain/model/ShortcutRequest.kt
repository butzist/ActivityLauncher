package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat

data class ShortcutRequest(
    val name: String,
    val component: ComponentName,
    val icon: IconCompat,
    val extras: Bundle? = null,
    val launcherPlugin: ComponentName? = null,
)
