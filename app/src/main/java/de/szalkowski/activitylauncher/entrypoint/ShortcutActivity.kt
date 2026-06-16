package de.szalkowski.activitylauncher.entrypoint

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : AppCompatActivity() {
    @Inject
    internal lateinit var activityLauncher: ActivityLauncher

    @Inject
    internal lateinit var intentSigner: IntentSigner

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val launchIntent =
                Intent.parseUri(intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT), 0)
            val signature =
                intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE).orEmpty()
            val asRoot = intent.action == ShortcutCreator.INTENT_LAUNCH_ROOT_SHORTCUT

            if (asRoot && !intentSigner.validateIntentSignature(launchIntent, signature)) {
                return
            }

            activityLauncher.launchActivity(
                launchIntent.component!!,
                asRoot,
                showToast = false,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ActivityCompat.finishAffinity(this)
        }
    }
}
