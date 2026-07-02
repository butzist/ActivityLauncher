package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.core.util.toIconCompat
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import javax.inject.Inject

class ActivityLauncherProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActivityLauncherProxy {
    private val pm: PackageManager = context.packageManager

    override fun launchActivity(request: LaunchRequest, plugin: ComponentName?) {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        if (plugin != null) {
            intent.component = plugin
        }

        val launchIntent = getActivityIntent(request.component, request.extras)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    override fun hasMultipleHandlers(): Boolean {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        val handlers = pm.queryIntentActivities(intent, 0)
        return handlers.size > 1
    }

    override fun getPlugins(): List<PluginInfo> {
        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
        val handlers = pm.queryIntentActivities(intent, 0)
        return handlers.map { handler ->
            val name = handler.loadLabel(pm).toString()
            val componentName = ComponentName(handler.activityInfo.packageName, handler.activityInfo.name)
            val icon = handler.loadIcon(pm).toIconCompat()
            PluginInfo(name, componentName, icon)
        }
    }
}
