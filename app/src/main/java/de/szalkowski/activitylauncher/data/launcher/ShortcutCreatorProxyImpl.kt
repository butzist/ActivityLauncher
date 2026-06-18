package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import javax.inject.Inject

class ShortcutCreatorProxyImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) : ShortcutCreatorProxy {
    override fun createLauncherIcon(
        activity: MyActivityInfo,
        optionalExtras: Bundle?,
    ) {
        val launchIntent = getActivityIntent(activity.componentName, optionalExtras)
        val icon = getActivityIconUseCase(activity.iconResourceName, activity.componentName)

        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_NAME, activity.name)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(0))
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_ICON, icon.toBundle())

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
