package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    private val activityLauncher: ActivityLauncher,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(componentName: ComponentName, asRoot: Boolean, showToast: Boolean = true) {
        activityLauncher.launchActivity(componentName, asRoot, showToast)
        recentsRepository.addActivity(componentName, asRoot)
    }
}
