package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
    private val shortcutsRepository: ShortcutsRepository,
) : ShortcutCreator {
    private val scope = CoroutineScope(Dispatchers.IO)

    override suspend fun createLauncherIcon(request: ShortcutRequest, shortcutId: Long?) {
        val id = shortcutId ?: shortcutsRepository.recordShortcut(request)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT)
        intent.component = ComponentName(
            context,
            ShortcutActivity::class.java,
        )

        intent.putExtra(
            ShortcutCreator.INTENT_EXTRA_INTENT,
            request.intent.toUri(Intent.URI_INTENT_SCHEME),
        )

        intent.putExtra(ShortcutCreator.INTENT_EXTRA_NAME, request.name)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ID, id)
        // Icon bundle is removed here to avoid PersistableBundle issues in ShortcutInfo

        val signature = intentSigner.signRequest(request)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)

        request.launcherPlugin?.let {
            intent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
        }

        val label = request.name.ifBlank { context.getString(R.string.app_name) }
        val shortcut = ShortcutInfoCompat.Builder(context, id.toString())
            .setShortLabel(label)
            .setIcon(request.icon)
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
