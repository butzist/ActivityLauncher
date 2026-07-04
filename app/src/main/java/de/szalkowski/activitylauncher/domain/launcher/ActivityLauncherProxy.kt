package de.szalkowski.activitylauncher.domain.launcher

import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.PluginInfo

interface ActivityLauncherProxy {
    fun launchActivity(request: LaunchRequest)

    fun hasMultipleHandlers(): Boolean
    fun getPlugins(): List<PluginInfo>

    companion object {
        const val INTENT_LAUNCH_ACTIVITY = "activitylauncher.intent.action.LAUNCH_ACTIVITY"
    }
}
