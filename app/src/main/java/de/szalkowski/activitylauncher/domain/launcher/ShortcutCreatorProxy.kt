package de.szalkowski.activitylauncher.domain.launcher

import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo

interface ShortcutCreatorProxy {
    fun createLauncherIcon(
        activity: MyActivityInfo,
        optionalExtras: Bundle? = null,
    )

    companion object {
        const val INTENT_CREATE_SHORTCUT = "activitylauncher.intent.action.CREATE_SHORTCUT"
    }
}
