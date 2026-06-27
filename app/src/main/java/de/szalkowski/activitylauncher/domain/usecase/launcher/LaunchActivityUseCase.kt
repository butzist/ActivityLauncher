package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    private val activityLauncher: ActivityLauncherProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(
        componentName: ComponentName,
        launchPlugin: ComponentName? = null,
    ) {
        Log.i("LaunchActivityUseCase", "Launching activity: ${componentName.flattenToShortString()}")
        activityLauncher.launchActivity(componentName, plugin = launchPlugin)
        recentsRepository.addActivity(componentName)
    }

    fun hasMultipleHandlers(): Boolean = activityLauncher.hasMultipleHandlers()
    fun getPlugins(): List<PluginInfo> = activityLauncher.getPlugins()
}
