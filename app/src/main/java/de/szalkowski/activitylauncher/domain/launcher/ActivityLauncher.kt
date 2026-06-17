package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.os.Bundle

interface ActivityLauncher {
    fun launchActivity(
        activity: ComponentName,
        optionalExtras: Bundle? = null,
    )
}
