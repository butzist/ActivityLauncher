package de.szalkowski.activitylauncher.domain.launcher

import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.SystemActivity

interface ShortcutCreatorProxy {
    fun createLauncherIcon(
        activity: SystemActivity,
        optionalExtras: Bundle? = null,
        useChooser: Boolean = false,
    )

    fun hasMultipleHandlers(): Boolean

    companion object {
        const val INTENT_CREATE_SHORTCUT = "de.szalkowski.activitylauncher.intent.action.CREATE_SHORTCUT"
    }
}
