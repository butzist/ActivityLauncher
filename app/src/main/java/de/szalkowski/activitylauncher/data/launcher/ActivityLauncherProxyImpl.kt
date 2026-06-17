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
    ) {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        intent.putExtra(ActivityLauncherProxy.INTENT_EXTRA_COMPONENT, activity.flattenToString())
        intent.putExtra(ActivityLauncherProxy.INTENT_EXTRA_EXTRAS, optionalExtras)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
