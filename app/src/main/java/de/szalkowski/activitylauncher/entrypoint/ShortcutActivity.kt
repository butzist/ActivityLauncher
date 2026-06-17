package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : AppCompatActivity() {
    @Inject
    internal lateinit var activityLauncher: ActivityLauncher

    @Inject
    internal lateinit var shortcutCreator: ShortcutCreator

    @Inject
    internal lateinit var intentSigner: IntentSigner

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            when (intent.action) {
                ShortcutCreator.INTENT_LAUNCH_SHORTCUT -> {
                    handleLaunchShortcut()
                    finish()
                }

                ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT -> {
                    if (checkPermission("de.szalkowski.activitylauncher.permission.CREATE_SHORTCUT")) {
                        handleCreateShortcut()
                        // On API 26+, requestPinShortcut requires the activity to be in the foreground.
                        // If we finish() immediately, the request might fail.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            finish()
                        }
                    } else {
                        finish()
                    }
                }

                ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY -> {
                    if (checkPermission("de.szalkowski.activitylauncher.permission.LAUNCH_ACTIVITY")) {
                        handleLaunchActivity()
                    }
                    finish()
                }

                else -> finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (intent.action == ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Give the system a moment to process the pinning request before finishing
            window.decorView.postDelayed({
                if (!isFinishing) finish()
            }, 500)
        }
    }

    private fun checkPermission(permission: String): Boolean {
        if (checkCallingOrSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return true
        }

        android.util.Log.e("ShortcutActivity", "Permission denied: $permission")
        return false
    }

    private fun handleLaunchShortcut() {
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return
        val launchIntent = Intent.parseUri(launchIntentStr, 0)
        val signature = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE).orEmpty()

        if (!intentSigner.validateIntentSignature(launchIntent, signature)) {
            return
        }

        activityLauncher.launchActivity(
            launchIntent.component!!,
            launchIntent.extras,
        )
    }

    private fun handleLaunchActivity() {
        val componentStr = intent.getStringExtra(ActivityLauncherProxy.INTENT_EXTRA_COMPONENT) ?: return
        val component = ComponentName.unflattenFromString(componentStr) ?: return
        val extras = intent.getBundleExtra(ActivityLauncherProxy.INTENT_EXTRA_EXTRAS)

        activityLauncher.launchActivity(
            component,
            extras,
        )
    }

    private fun handleCreateShortcut() {
        val appName = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_NAME) ?: ""
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: ""
        val launchIntent = Intent.parseUri(launchIntentStr, 0)
        val iconBundle = intent.getBundleExtra(ShortcutCreator.INTENT_EXTRA_ICON) ?: return
        val iconCompat = IconCompat.createFromBundle(iconBundle) ?: return
        val component = launchIntent.component ?: return

        val activityInfo = MyActivityInfo(
            component,
            appName,
            null, // iconResourceName
            false, // isPrivate
        )

        shortcutCreator.createLauncherIcon(activityInfo, iconCompat, launchIntent.extras)
    }
}
