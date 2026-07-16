package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.content.Context
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val shortcutCreatorProxy: ShortcutCreatorProxy,
    private val recentsRepository: RecentsRepository,
    private val shortcutsRepository: ShortcutsRepository,
    private val createShortcutIntentUseCase: CreateShortcutIntentUseCase,
) {
    suspend operator fun invoke(
        request: ShortcutRequest,
        shortcutId: String? = null,
        shortcutPlugin: ComponentName? = null,
        context: Context? = null,
    ) {
        Log.i(
            "CreateShortcutUseCase",
            "Creating shortcut: ${request.intent.component?.flattenToShortString()}",
        )

        recentsRepository.addActivity(request, updateMetadata = true)
        val id = if (shortcutId != null) {
            shortcutsRepository.updateShortcut(shortcutId, request)
            shortcutId
        } else {
            shortcutsRepository.recordShortcut(request)
        }

        val launchIntent = createShortcutIntentUseCase(request, id)
        val proxyRequest =
            ShortcutProxyRequest(request.name, request.icon, launchIntent, source = request.source)
        val useProxy =
            request.source != LaunchSource.PROXY && (shortcutPlugin != null || shortcutCreatorProxy.hasMultipleHandlers())

        if (useProxy) {
            shortcutCreatorProxy.createLauncherIcon(proxyRequest, shortcutPlugin, context)
        } else {
            shortcutCreator.createLauncherIcon(proxyRequest, id, context)
        }
    }

    fun hasMultipleHandlers(): Boolean = shortcutCreatorProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = shortcutCreatorProxy.getPlugins()
}
