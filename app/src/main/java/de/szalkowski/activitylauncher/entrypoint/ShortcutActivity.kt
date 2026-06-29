package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : AppCompatActivity() {
    @Inject
    internal lateinit var activityLauncher: ActivityLauncher

    @Inject
    internal lateinit var shortcutCreator: ShortcutCreator

    @Inject
    internal lateinit var intentSigner: IntentSigner

    @Inject
    internal lateinit var viewIntentParser: ViewIntentParser

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

        Log.e("ShortcutActivity", "Permission denied: $permission")
        return false
    }

    private fun redirectToMain(componentName: ComponentName) {
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.putExtra(MainActivity.EXTRA_ACTIVITY_COMPONENT_NAME, componentName)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(mainIntent)
    }

    private fun handleLaunchShortcut() {
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return
        val launchIntent = viewIntentParser.parseShortcutIntent(launchIntentStr) ?: return
        val signature = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE).orEmpty()
        val launchPlugin = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN)

        if (!intentSigner.validateIntentSignature(launchIntent, signature, launchPlugin)) {
            Log.e("ShortcutActivity", "Invalid signature for shortcut")
            launchIntent.component?.let { redirectToMain(it) }
            return
        }

        if (launchPlugin != null) {
            val component = ComponentName.unflattenFromString(launchPlugin)
            if (component != null) {
                val delegationIntent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY)
                delegationIntent.component = component
                delegationIntent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
                delegationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching {
                    startActivity(delegationIntent)
                }.onFailure {
                    Toast.makeText(this, R.string.error_invalid_activity_link, Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        activityLauncher.launchActivity(
            launchIntent.component!!,
            launchIntent.extras,
        )
    }

    private fun handleLaunchActivity() {
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return
        val launchIntent = viewIntentParser.parseShortcutIntent(launchIntentStr) ?: return

        activityLauncher.launchActivity(
            launchIntent.component!!,
            launchIntent.extras,
        )
    }

    private fun handleCreateShortcut() {
        val appName = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_NAME) ?: ""
        val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: ""
        val launchIntent = viewIntentParser.parseShortcutIntent(launchIntentStr) ?: return
        val iconBundle = intent.getBundleExtra(ShortcutCreator.INTENT_EXTRA_ICON) ?: return
        val iconCompat = IconCompat.createFromBundle(iconBundle) ?: return
        val component = launchIntent.component ?: return
        val launchPlugin = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN)

        val activityInfo = SystemActivity(
            component,
            appName,
            null, // iconResourceName
            false, // isPrivate
        )

        val extras = launchIntent.extras ?: Bundle()
        if (launchPlugin != null) {
            extras.putString(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, launchPlugin)
        }

        shortcutCreator.createLauncherIcon(activityInfo.name, activityInfo.componentName, iconCompat, extras)
    }
}
