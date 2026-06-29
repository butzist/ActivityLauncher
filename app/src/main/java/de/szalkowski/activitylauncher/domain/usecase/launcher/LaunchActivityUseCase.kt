package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
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
    operator fun invoke(request: LaunchRequest, launchPlugin: ComponentName? = null) {
        Log.i("LaunchActivityUseCase", "Launching activity: ${request.component.flattenToShortString()}")
        if (launchPlugin != null || activityLauncherProxy.hasMultipleHandlers()) {
            activityLauncherProxy.launchActivity(request, launchPlugin)
        } else {
            activityLauncher.launchActivity(request)
        }
        recentsRepository.addActivity(request.component)
    }

    fun hasMultipleHandlers(): Boolean = activityLauncherProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = activityLauncherProxy.getPlugins()
}
