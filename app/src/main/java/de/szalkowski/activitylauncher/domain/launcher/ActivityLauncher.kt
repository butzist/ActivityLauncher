package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName

interface ActivityLauncher {
    fun launchActivity(
        activity: ComponentName,
        asRoot: Boolean,
        showToast: Boolean,
    )
}
