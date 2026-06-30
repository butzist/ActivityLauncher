package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : AppCompatActivity() {
    @Inject
    internal lateinit var intentSigner: IntentSigner

    @Inject
    internal lateinit var viewIntentParser: ViewIntentParser

    @Inject
    internal lateinit var launchActivityUseCase: LaunchActivityUseCase

    @Inject
    internal lateinit var activityLauncher: ActivityLauncher

    @Inject
    internal lateinit var shortCutCreator: ShortcutCreator

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
            }, 1000)
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
        val request = viewIntentParser.parseShortcutRequest(intent) ?: return
        val signature = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE).orEmpty()

        if (!intentSigner.validateRequestSignature(request, signature)) {
            Log.e("ShortcutActivity", "Invalid signature for shortcut")
            redirectToMain(request.component)
            return
        }

        launchActivityUseCase.invoke(LaunchRequest(request.component, request.extras), request.launcherPlugin)
    }

    private fun handleLaunchActivity() {
        val request = viewIntentParser.parseLaunchRequest(intent) ?: return
        activityLauncher.launchActivity(request)
    }

    private fun handleCreateShortcut() {
        val request = viewIntentParser.parseShortcutRequest(intent) ?: return
        shortCutCreator.createLauncherIcon(request)
    }
}
