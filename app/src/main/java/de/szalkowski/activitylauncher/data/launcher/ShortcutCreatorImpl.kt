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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
) : ShortcutCreator {

    override fun createLauncherIcon(request: ShortcutRequest) {
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT)
        intent.component = ComponentName(
            context.packageName,
            "de.szalkowski.activitylauncher.ShortcutActivity",
        )

        intent.putExtra(
            ShortcutCreator.INTENT_EXTRA_INTENT,
            request.intent.toUri(Intent.URI_INTENT_SCHEME),
        )

        val signature = intentSigner.signRequest(request)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)

        request.launcherPlugin?.let {
            intent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
        }

        val label = request.name.ifBlank { context.getString(R.string.app_name) }
        val shortcut = ShortcutInfoCompat.Builder(context, UUID.randomUUID().toString())
            .setShortLabel(label)
            .setIcon(request.icon)
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
