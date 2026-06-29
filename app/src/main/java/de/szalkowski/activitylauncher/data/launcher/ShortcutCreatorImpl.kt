package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) : ShortcutCreator {

    override fun createLauncherIcon(request: ShortcutRequest) {
        val launchIntent = Intent(Intent.ACTION_MAIN)
        launchIntent.component = request.component
        request.extras?.let {
            launchIntent.putExtras(it)
        }
        request.launcherPlugin?.let {
            launchIntent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
        }

        val shortcut = ShortcutInfoCompat.Builder(context, request.component.flattenToShortString())
            .setShortLabel(request.name)
            .setIcon(request.icon)
            .setIntent(launchIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
