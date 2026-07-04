package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    private val activityLauncher: ActivityLauncher,
    private val activityLauncherProxy: ActivityLauncherProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(request: LaunchRequest) {
        val component = request.intent.component ?: return
        Log.i("LaunchActivityUseCase", "Launching activity: ${component.flattenToShortString()}")
        if (request.launcherPlugin != null || activityLauncherProxy.hasMultipleHandlers()) {
            activityLauncherProxy.launchActivity(request)
        } else {
            activityLauncher.launchActivity(request)
        }
        recentsRepository.addActivity(component)
    }

    fun hasMultipleHandlers(): Boolean = activityLauncherProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = activityLauncherProxy.getPlugins()
}
