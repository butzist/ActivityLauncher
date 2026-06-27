package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.PluginInfo

interface ActivityLauncherProxy {
    fun launchActivity(
        activity: ComponentName,
        optionalExtras: Bundle? = null,
        plugin: ComponentName? = null,
    )

    fun hasMultipleHandlers(): Boolean
    fun getPlugins(): List<PluginInfo>

    companion object {
        const val INTENT_LAUNCH_ACTIVITY = "activitylauncher.intent.action.LAUNCH_ACTIVITY"
    }
}
