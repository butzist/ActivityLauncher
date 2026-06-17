package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.os.Bundle
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val recentsRepository: RecentsRepository,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) {
    operator fun invoke(activity: MyActivityInfo, asRoot: Boolean, optionalExtras: Bundle? = null) {
        val icon = getActivityIconUseCase(activity.iconResourceName, activity.componentName)
        if (asRoot) {
            shortcutCreator.createRootLauncherIcon(activity, icon, optionalExtras)
        } else {
            shortcutCreator.createLauncherIcon(activity, icon, optionalExtras)
        }
        recentsRepository.addActivity(activity.componentName, asRoot)
    }
}
