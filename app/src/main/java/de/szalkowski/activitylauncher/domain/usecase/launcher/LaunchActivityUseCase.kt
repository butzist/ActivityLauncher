package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.Context
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
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
    operator fun invoke(request: LaunchRequest, context: Context? = null) {
        val component = request.intent.component ?: return

        if (request.source != LaunchSource.PROXY) {
            Log.i(
                "LaunchActivityUseCase",
                "Recording launch for: ${component.flattenToShortString()}",
            )
            val shortcutRequest =
                request.toShortcutRequest(packageRepository, getActivityIconUseCase)
            recentsRepository.addActivity(
                shortcutRequest,
                updateMetadata = request.source == LaunchSource.PRIMARY,
            )
        }

        val useProxy =
            request.source != LaunchSource.PROXY && (request.launcherPlugin != null || (request.source == LaunchSource.PRIMARY && activityLauncherProxy.hasMultipleHandlers()))

        if (useProxy) {
            Log.i("LaunchActivityUseCase", "Proxy launch for: ${component.flattenToShortString()}")
            activityLauncherProxy.launchActivity(request, context)
        } else {
            Log.i("LaunchActivityUseCase", "Direct launch for: ${component.flattenToShortString()}")
            activityLauncher.launchActivity(request, context)
        }
    }

    fun hasMultipleHandlers(): Boolean = activityLauncherProxy.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = activityLauncherProxy.getPlugins()
}
