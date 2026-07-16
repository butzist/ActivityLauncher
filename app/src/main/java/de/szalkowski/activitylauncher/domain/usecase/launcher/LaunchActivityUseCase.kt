package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    private val activityLauncher: ActivityLauncher,
    private val activityLauncherProxy: ActivityLauncherProxy,
    private val recentsRepository: RecentsRepository,
    private val packageRepository: PackageRepository,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) {
    operator fun invoke(request: LaunchRequest) {
        val component = request.intent.component ?: return
        val isPrimaryRequest = request.name != null && request.icon != null
        val shortcutRequest = request.toShortcutRequest(packageRepository, getActivityIconUseCase)

        Log.i("LaunchActivityUseCase", "Recording launch for: ${component.flattenToShortString()}")
        recentsRepository.addActivity(shortcutRequest)

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
