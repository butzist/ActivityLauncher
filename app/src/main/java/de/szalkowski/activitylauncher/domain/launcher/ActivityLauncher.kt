package de.szalkowski.activitylauncher.domain.launcher

import de.szalkowski.activitylauncher.domain.model.LaunchRequest

interface ActivityLauncher {
    fun launchActivity(request: LaunchRequest)
}
