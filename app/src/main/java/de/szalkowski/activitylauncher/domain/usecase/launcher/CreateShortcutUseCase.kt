package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.os.Bundle
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    private val shortcutCreator: ShortcutCreator,
    private val shortcutCreatorProxy: ShortcutCreatorProxy,
) {
    operator fun invoke(activity: SystemActivity, optionalExtras: Bundle? = null, useChooser: Boolean = false) {
        if (shortcutCreatorProxy.hasMultipleHandlers()) {
            shortcutCreatorProxy.createLauncherIcon(activity, optionalExtras, useChooser)
        } else {
            shortcutCreator.createLauncherIcon(activity, optionalExtras, useChooser)
        }
    }

    fun hasMultipleHandlers(): Boolean {
        return shortcutCreatorProxy.hasMultipleHandlers()
    }
}
