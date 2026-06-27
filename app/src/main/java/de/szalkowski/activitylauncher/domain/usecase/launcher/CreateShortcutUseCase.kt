package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.os.Bundle
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val shortcutCreatorProxy: ShortcutCreatorProxy,
) {
    operator fun invoke(
        activity: SystemActivity,
        optionalExtras: Bundle? = null,
        shortcutPlugin: ComponentName? = null,
        launchPlugin: ComponentName? = null,
    ) {
        if (shortcutPlugin != null || shortcutCreatorProxy.hasMultipleHandlers()) {
            shortcutCreatorProxy.createLauncherIcon(activity, optionalExtras, shortcutPlugin, launchPlugin)
        } else {
            shortcutCreator.createLauncherIcon(activity, optionalExtras)
        }
    }

    fun hasMultipleHandlers(): Boolean {
        return shortcutCreatorProxy.hasMultipleHandlers()
    }

    fun getPlugins(): List<PluginInfo> {
        return shortcutCreatorProxy.getPlugins()
    }
}
