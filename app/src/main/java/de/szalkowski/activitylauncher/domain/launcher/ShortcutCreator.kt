package de.szalkowski.activitylauncher.domain.launcher

import android.graphics.drawable.Drawable
import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo

interface ShortcutCreator {
    fun createLauncherIcon(
        activity: MyActivityInfo,
        icon: Drawable,
        optionalExtras: Bundle? = null,
    )

    fun createRootLauncherIcon(
        activity: MyActivityInfo,
        icon: Drawable,
        optionalExtras: Bundle? = null,
    )

    companion object {
        const val INTENT_LAUNCH_SHORTCUT = "activitylauncher.intent.action.LAUNCH_SHORTCUT"
        const val INTENT_LAUNCH_ROOT_SHORTCUT =
            "activitylauncher.intent.action.LAUNCH_ROOT_SHORTCUT"

        const val INTENT_EXTRA_INTENT = "extra_intent"
        const val INTENT_EXTRA_SIGNATURE = "sign"
    }
}
