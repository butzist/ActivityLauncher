package de.szalkowski.activitylauncher.entrypoint

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.launcher.ShortcutUpdateConfirmation
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutUpdateConfirmationImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShortcutUpdateConfirmation {
    companion object {
        private var continuation: CompletableDeferred<Boolean>? = null

        fun onResult(update: Boolean) {
            continuation?.complete(update)
            continuation = null
        }
    }

    override suspend fun confirmUpdate(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        continuation = deferred

        val intent = Intent(context, ShortcutUpdateActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        return deferred.await()
    }
}
