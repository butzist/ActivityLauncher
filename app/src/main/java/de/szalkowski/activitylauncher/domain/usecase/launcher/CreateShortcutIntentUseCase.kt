package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.data.database.SerializationUtils
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import javax.inject.Inject

class CreateShortcutIntentUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
) {
    operator fun invoke(request: ShortcutRequest, shortcutId: String): Intent {
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT)
        intent.component = ComponentName(context, ShortcutActivity::class.java)

        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ID, shortcutId)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, SerializationUtils.intentToUri(request.intent))

        val signature = intentSigner.signRequest(request)
        intent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)

        request.launcherPlugin?.let {
            intent.putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, it.flattenToString())
        }

        return intent
    }
}
