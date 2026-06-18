package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import javax.inject.Inject

class ActivityLauncherProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActivityLauncherProxy {
    override fun launchActivity(
        activity: ComponentName,
        optionalExtras: Bundle?,
        useChooser: Boolean,
    ) {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        intent.putExtra(ActivityLauncherProxy.INTENT_EXTRA_COMPONENT, activity.flattenToString())
        intent.putExtra(ActivityLauncherProxy.INTENT_EXTRA_EXTRAS, optionalExtras)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (useChooser) {
            val chooser = Intent.createChooser(intent, "…")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } else {
            context.startActivity(intent)
        }
    }

    override fun hasMultipleHandlers(): Boolean {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        val handlers = context.packageManager.queryIntentActivities(intent, 0)
        return handlers.size > 1
    }
}
