package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

interface ShortcutCreatorProxy {
    fun createLauncherIcon(request: ShortcutRequest, plugin: ComponentName? = null)

    fun hasMultipleHandlers(): Boolean
    fun getPlugins(): List<PluginInfo>

    companion object {
        const val INTENT_CREATE_SHORTCUT = "activitylauncher.intent.action.CREATE_SHORTCUT"
    }
}
