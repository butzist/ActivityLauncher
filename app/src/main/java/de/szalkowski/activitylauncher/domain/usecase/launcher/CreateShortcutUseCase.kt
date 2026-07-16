package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val shortcutCreatorProxy: ShortcutCreatorProxy,
    private val recentsRepository: RecentsRepository,
    private val shortcutsRepository: ShortcutsRepository,
) {
    suspend operator fun invoke(request: ShortcutRequest, shortcutPlugin: ComponentName? = null) {
        Log.i("CreateShortcutUseCase", "Creating shortcut: ${request.intent.component?.flattenToShortString()}")

        recentsRepository.addActivity(request)
        val id = shortcutsRepository.recordShortcut(request)

        if (shortcutPlugin != null || shortcutCreatorProxy.hasMultipleHandlers()) {
            shortcutCreatorProxy.createLauncherIcon(request, shortcutPlugin, id)
        } else {
            shortcutCreator.createLauncherIcon(request, id)
        }
    }

    fun hasMultipleHandlers(): Boolean = shortcutCreatorProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = shortcutCreatorProxy.getPlugins()
}
