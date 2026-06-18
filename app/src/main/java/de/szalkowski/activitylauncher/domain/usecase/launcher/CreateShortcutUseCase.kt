package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.os.Bundle
import android.util.Log
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreatorProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(activity: MyActivityInfo, optionalExtras: Bundle? = null, useChooser: Boolean = false) {
        Log.i("CreateShortcutUseCase", "Creating shortcut for: ${activity.name}")
        shortcutCreator.createLauncherIcon(activity, optionalExtras, useChooser)
        recentsRepository.addActivity(activity.componentName)
    }

    fun hasMultipleHandlers(): Boolean = shortcutCreator.hasMultipleHandlers()
}
