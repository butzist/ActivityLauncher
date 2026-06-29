package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.core.util.toIconCompat
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import javax.inject.Inject

class ShortcutCreatorProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
) : ShortcutCreatorProxy {
    private val pm: PackageManager = context.packageManager

    override fun createLauncherIcon(request: ShortcutRequest, plugin: ComponentName?) {
        val launchIntent = getActivityIntent(request.component, request.extras)

        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        if (plugin != null) {
            intent.component = plugin
        }
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_NAME, request.name)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_ICON, request.icon.toBundle())

        val signature = intentSigner.signRequest(request)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ACTIVITY, ComponentName(context, ShortcutActivity::class.java).flattenToString())
        request.launcherPlugin?.let {
            intent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun hasMultipleHandlers(): Boolean {
        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        val handlers = pm.queryIntentActivities(intent, 0)
        return handlers.size > 1
    }

    override fun getPlugins(): List<PluginInfo> {
        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        val handlers = pm.queryIntentActivities(intent, 0)
        return handlers.map { handler ->
            val name = handler.loadLabel(pm).toString()
            val componentName = ComponentName(handler.activityInfo.packageName, handler.activityInfo.name)
            val icon = handler.loadIcon(pm).toIconCompat()
            PluginInfo(name, componentName, icon)
        }
    }
}
