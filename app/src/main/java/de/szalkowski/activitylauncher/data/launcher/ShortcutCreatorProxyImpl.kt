package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.toIconCompat
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ShortcutCreatorProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
    private val shortcutsRepository: ShortcutsRepository,
) : ShortcutCreatorProxy {
    private val pm: PackageManager = context.packageManager
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun createLauncherIcon(request: ShortcutRequest, plugin: ComponentName?, shortcutId: Long?) {
        val id = shortcutId ?: shortcutsRepository.recordShortcut(request)

        val launchShortcutIntent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            component = ComponentName(context, ShortcutActivity::class.java)
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, request.intent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, request.name)
            putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ID, id)
            val signature = intentSigner.signRequest(request)
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            request.launcherPlugin?.let {
                putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
            }
        }

        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        if (plugin != null) {
            intent.component = plugin
        }

        intent.putExtra(ShortcutCreator.INTENT_EXTRA_NAME, request.name)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchShortcutIntent.toUri(Intent.URI_INTENT_SCHEME))
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ACTIVITY, ComponentName(context, ShortcutActivity::class.java).flattenToString())

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
