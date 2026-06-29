package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val shortcutCreatorProxy: ShortcutCreatorProxy,
) {
    operator fun invoke(request: ShortcutRequest, shortcutPlugin: ComponentName? = null) {
        Log.i("CreateShortcutUseCase", "Creating shortcut: ${request.component.flattenToShortString()}")
        if (shortcutPlugin != null || shortcutCreatorProxy.hasMultipleHandlers()) {
            shortcutCreatorProxy.createLauncherIcon(request, shortcutPlugin)
        } else {
            shortcutCreator.createLauncherIcon(request)
        }
    }

    fun hasMultipleHandlers(): Boolean = shortcutCreatorProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = shortcutCreatorProxy.getPlugins()
}
