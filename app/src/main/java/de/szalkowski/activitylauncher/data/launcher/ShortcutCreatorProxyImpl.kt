package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import javax.inject.Inject

class ShortcutCreatorProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActivityIconUseCase: GetActivityIconUseCase,
    private val intentSigner: IntentSigner,
) : ShortcutCreatorProxy {
    override fun createLauncherIcon(
        activity: MyActivityInfo,
        optionalExtras: Bundle?,
        useChooser: Boolean,
    ) {
        val launchIntent = getActivityIntent(activity.componentName, optionalExtras)
        val icon = getActivityIconUseCase(activity.iconResourceName, activity.componentName)

        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_NAME, activity.name)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_ICON, icon.toBundle())

        val launchPlugin = optionalExtras?.getString(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN)
        val signature = intentSigner.signIntent(launchIntent, launchPlugin)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ACTIVITY, ComponentName(context, ShortcutActivity::class.java).flattenToString())
        if (launchPlugin != null) {
            intent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, launchPlugin)
        }

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
        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        val handlers = context.packageManager.queryIntentActivities(intent, 0)
        return handlers.size > 1
    }
}
