package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) : ShortcutCreator {

    override fun createLauncherIcon(
        activity: SystemActivity,
        optionalExtras: Bundle?,
    ) {
        val icon = getActivityIconUseCase(activity.iconResourceName, activity.componentName)
        createLauncherIcon(activity.name, activity.componentName, icon, optionalExtras)
    }

    override fun createLauncherIcon(
        name: String,
        componentName: android.content.ComponentName,
        icon: IconCompat,
        optionalExtras: Bundle?,
    ) {
        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.component = componentName
        if (optionalExtras != null) {
            launchIntent.putExtras(optionalExtras)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, componentName.flattenToShortString())
            .setShortLabel(name)
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
