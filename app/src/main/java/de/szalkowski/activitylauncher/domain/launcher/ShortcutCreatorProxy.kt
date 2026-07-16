package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.content.Context
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest

interface ShortcutCreatorProxy {
    suspend fun createLauncherIcon(request: ShortcutProxyRequest, plugin: ComponentName? = null, context: Context? = null)

    fun hasMultipleHandlers(): Boolean
    fun getPlugins(): List<PluginInfo>

    companion object {
        const val INTENT_CREATE_SHORTCUT = "activitylauncher.intent.action.CREATE_SHORTCUT"
    }
}
