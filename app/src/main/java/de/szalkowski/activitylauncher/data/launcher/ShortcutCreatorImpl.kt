package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutUpdateConfirmation
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shortcutUpdateConfirmation: ShortcutUpdateConfirmation,
) : ShortcutCreator {

    override suspend fun createLauncherIcon(request: ShortcutProxyRequest, shortcutId: String, context: Context?) {
        val launchContext = context ?: this.context
        val label = request.name.ifBlank { launchContext.getString(R.string.app_name) }
        val shortcut = ShortcutInfoCompat.Builder(launchContext, shortcutId)
            .setShortLabel(label)
            .setIcon(request.icon.toShortcutIcon(launchContext))
            .setIntent(request.intent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val exists = ShortcutManagerCompat.getShortcuts(launchContext, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .any { it.id == shortcutId }

            if (exists) {
                val update = shortcutUpdateConfirmation.confirmUpdate()
                if (update) {
                    ShortcutManagerCompat.updateShortcuts(launchContext, listOf(shortcut))
                    return
                }
            }
        }

        ShortcutManagerCompat.requestPinShortcut(launchContext, shortcut, null)
    }
}
