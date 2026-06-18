package de.szalkowski.activitylauncher.domain.launcher

import android.content.ComponentName
import android.os.Bundle

interface ActivityLauncherProxy {
    fun launchActivity(
        activity: ComponentName,
        optionalExtras: Bundle? = null,
        useChooser: Boolean = false,
    )

    fun hasMultipleHandlers(): Boolean

    companion object {
        const val INTENT_LAUNCH_ACTIVITY = "activitylauncher.intent.action.LAUNCH_ACTIVITY"
        const val INTENT_EXTRA_COMPONENT = "extra_component"
        const val INTENT_EXTRA_EXTRAS = "extra_extras"
    }
}
