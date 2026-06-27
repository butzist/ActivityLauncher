package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.SystemActivity

interface ShortcutCreatorProxy {
    fun createLauncherIcon(
        activity: SystemActivity,
        optionalExtras: Bundle? = null,
        plugin: ComponentName? = null,
        launchPlugin: ComponentName? = null,
    )

    fun hasMultipleHandlers(): Boolean
    fun getPlugins(): List<PluginInfo>

    companion object {
        const val INTENT_CREATE_SHORTCUT = "de.szalkowski.activitylauncher.intent.action.CREATE_SHORTCUT"
    }
}
