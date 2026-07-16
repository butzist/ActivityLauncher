package de.szalkowski.activitylauncher.domain.launcher

import android.content.Context
import de.szalkowski.activitylauncher.domain.model.LaunchRequest

interface ActivityLauncher {
    fun launchActivity(request: LaunchRequest, context: Context? = null)
}
