package de.szalkowski.activitylauncher.domain.launcher

import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo

interface ShortcutCreator {
    fun createLauncherIcon(
        activity: MyActivityInfo,
        icon: IconCompat? = null,
        optionalExtras: Bundle? = null,
    )

    companion object {
        const val INTENT_LAUNCH_SHORTCUT = "activitylauncher.intent.action.LAUNCH_SHORTCUT"
        const val INTENT_EXTRA_INTENT = "extra_intent"
        const val INTENT_EXTRA_NAME = "extra_name"
        const val INTENT_EXTRA_ICON = "extra_icon"
        const val INTENT_EXTRA_SIGNATURE = "sign"
        const val INTENT_EXTRA_SHORTCUT_ACTIVITY = "extra_shortcut_activity"
        const val INTENT_EXTRA_LAUNCH_PLUGIN = "extra_launch_plugin"
    }
}
