package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    private val activityLauncher: ActivityLauncher,
    private val activityLauncherProxy: ActivityLauncherProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(request: LaunchRequest) {
        val component = request.intent.component ?: return
        val isPrimaryRequest = request.name != null && request.icon != null

        if (isPrimaryRequest) {
            Log.i(
                "LaunchActivityUseCase",
                "Primary launch request for: ${component.flattenToShortString()}",
            )
            recentsRepository.addActivity(
                ShortcutRequest(
                    name = request.name,
                    intent = request.intent,
                    icon = request.icon,
                    launcherPlugin = request.launcherPlugin,
                ),
            )
        }

        val useProxy = request.launcherPlugin != null || (isPrimaryRequest && activityLauncherProxy.hasMultipleHandlers())
        if (useProxy) {
            Log.i("LaunchActivityUseCase", "Proxy launch for: ${component.flattenToShortString()}")
            activityLauncherProxy.launchActivity(request)
        } else {
            Log.i("LaunchActivityUseCase", "Direct launch for: ${component.flattenToShortString()}")
            activityLauncher.launchActivity(request)
        }
    }

    fun hasMultipleHandlers(): Boolean = activityLauncherProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = activityLauncherProxy.getPlugins()
}
