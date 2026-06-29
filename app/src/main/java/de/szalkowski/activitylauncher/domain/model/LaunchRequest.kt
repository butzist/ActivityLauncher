package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.os.Bundle

data class LaunchRequest(
    val component: ComponentName,
    val extras: Bundle? = null,
)
