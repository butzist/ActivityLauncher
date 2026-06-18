package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.core.util.toBitmap
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import javax.inject.Inject

class ShortcutCreatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentSigner: IntentSigner,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) : ShortcutCreator {
    override fun createLauncherIcon(
        activity: MyActivityInfo,
        icon: IconCompat?,
        optionalExtras: Bundle?,
    ) {
        val appName = activity.name
        val launchIntent = getActivityIntent(activity.componentName, optionalExtras)
        val finalIcon = icon ?: getActivityIconUseCase(activity.iconResourceName, activity.componentName)

        if (Build.VERSION.SDK_INT >= 26) {
            doCreateShortcut(appName, launchIntent, finalIcon)
        } else {
            val bitmap = finalIcon.loadDrawable(context)!!.toBitmap()
            doCreateShortcutLegacy(appName, launchIntent, bitmap)
        }
    }

    @Suppress("DEPRECATION")
    private fun doCreateShortcutLegacy(
        appName: String,
        intent: Intent,
        iconBitmap: Bitmap,
    ) {
        val shortcutIntent = Intent()
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, iconBitmap)
        shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT")
        context.sendBroadcast(shortcutIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun doCreateShortcut(
        appName: String,
        intent: Intent,
        iconCompat: IconCompat,
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)!!
        if (shortcutManager.isRequestPinShortcutSupported) {
            val icon = iconCompat.toIcon(context)
            val shortcutIntent = createShortcutIntent(intent)
            val shortcutInfo =
                ShortcutInfo.Builder(context, appName).setShortLabel(appName).setLongLabel(appName)
                    .setIcon(icon).setIntent(shortcutIntent).build()

            try {
                shortcutManager.requestPinShortcut(shortcutInfo, null)
            } catch (e: Exception) {
                // In some environments (like some tests), this might still throw even if foregrounded
                // We re-throw it if it's an IllegalStateException to allow the test to catch it,
                // or just log it if we want to be safe.
                if (e is IllegalStateException) {
                    throw e
                }
                e.printStackTrace()
                Toast.makeText(context, R.string.error_creating_shortcut, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, R.string.error_verbose_pin_shortcut, Toast.LENGTH_LONG).show()
        }
    }

    private fun createShortcutIntent(intent: Intent): Intent {
        val shortcutIntent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT)
        shortcutIntent.setPackage(context.packageName)
        shortcutIntent.putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, intent.toUri(0))

        val signature = intentSigner.signIntent(intent)
        shortcutIntent.putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)

        return shortcutIntent
    }
}
