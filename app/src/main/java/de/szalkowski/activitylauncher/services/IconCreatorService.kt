package de.szalkowski.activitylauncher.services

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.qualifiers.ActivityContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.internal.getActivityIntent
import javax.inject.Inject

interface IconCreatorService {
    fun createLauncherIcon(activity: MyActivityInfo, optionalExtras: Bundle? = null)
    fun createRootLauncherIcon(activity: MyActivityInfo, optionalExtras: Bundle? = null)

    companion object {
        const val INTENT_LAUNCH_SHORTCUT = "activitylauncher.intent.action.LAUNCH_SHORTCUT"
        const val INTENT_LAUNCH_ROOT_SHORTCUT = "activitylauncher.intent.action.LAUNCH_ROOT_SHORTCUT"

        const val INTENT_EXTRA_INTENT = "extra_intent"
        const val INTENT_EXTRA_SIGNATURE = "sign"
    }
}

class IconCreatorServiceImpl @Inject constructor(
    @ActivityContext private val context: Context,
    private val signingService: IntentSigningService
) : IconCreatorService {
    override fun createLauncherIcon(activity: MyActivityInfo, optionalExtras: Bundle?) {
        createLauncherIcon(activity, optionalExtras, false)
    }

    override fun createRootLauncherIcon(activity: MyActivityInfo, optionalExtras: Bundle?) {
        createLauncherIcon(activity, optionalExtras, true)
    }

    private fun createLauncherIcon(activity: MyActivityInfo, optionalExtras: Bundle?, asRoot: Boolean) {
        val pack = extractIconPackageName(activity)
        val intent = getActivityIntent(activity.componentName, optionalExtras)

        // Use bitmap version, if icon from different package is used
        if (pack != null && pack != activity.componentName.packageName) {
            createShortcut(activity.name, intent, activity.icon, asRoot, null)
        } else {
            createShortcut(activity.name, intent, activity.icon, asRoot, activity.iconResourceName)
        }
    }

    private fun extractIconPackageName(
        activity: MyActivityInfo,
    ): String? {
        if (activity.iconResourceName == null) return null

        val indexOfSeparator = activity.iconResourceName.indexOf(':')
        if (indexOfSeparator < 0) {
            return null
        }

        return activity.iconResourceName.substring(0, indexOfSeparator)
    }

    /**
     * Got reference from stackoverflow.com URL:
     * https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview
     * https://stackoverflow.com/questions/46130594/android-get-apps-adaptive-icons-from-package-manager
     */
    @TargetApi(26)
    private fun getIconFromDrawable(drawable: Drawable): Icon {
        if (drawable is AdaptiveIconDrawable) {
            val backgroundDr = drawable.background
            val foregroundDr = drawable.foreground
            val drr = arrayOfNulls<Drawable>(2)
            drr[0] = backgroundDr
            drr[1] = foregroundDr
            val layerDrawable = LayerDrawable(drr)
            val width = layerDrawable.intrinsicWidth
            val height = layerDrawable.intrinsicHeight
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
            layerDrawable.draw(canvas)
            return Icon.createWithAdaptiveBitmap(bitmap)
        }
        if (drawable is BitmapDrawable) {
            return Icon.createWithBitmap(drawable.bitmap)
        }
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Icon.createWithBitmap(bmp)
    }

    private fun createShortcut(
        appName: String, intent: Intent, draw: Drawable, asRoot: Boolean, iconResourceName: String?
    ) {
        Toast.makeText(
            context, String.format(
                context.getText(R.string.creating_application_shortcut).toString(), appName
            ), Toast.LENGTH_LONG
        ).show()
        if (Build.VERSION.SDK_INT >= 26) {
            doCreateShortcut(appName, intent, asRoot, draw)
        } else {
            doCreateShortcut(appName, intent, asRoot, iconResourceName)
        }
    }

    private fun doCreateShortcut(
        appName: String, intent: Intent, asRoot: Boolean, iconResourceName: String?
    ) {
        val shortcutIntent = Intent()
        if (asRoot) {
            // wrap only if root access needed
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, createShortcutIntent(intent, true))
        } else {
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        }
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName)
        if (iconResourceName != null) {
            val ir = ShortcutIconResource()
            if (intent.component == null) {
                ir.packageName = intent.getPackage()
            } else {
                ir.packageName = intent.component!!.packageName
            }
            ir.resourceName = iconResourceName
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir)

        }
        shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT")
        context.sendBroadcast(shortcutIntent)
    }

    @TargetApi(26)
    private fun doCreateShortcut(
        appName: String, intent: Intent, asRoot: Boolean, draw: Drawable
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)!!
        if (shortcutManager.isRequestPinShortcutSupported) {
            val icon = getIconFromDrawable(draw)
            val shortcutIntent = createShortcutIntent(intent, asRoot)
            val shortcutInfo =
                ShortcutInfo.Builder(context, appName).setShortLabel(appName).setLongLabel(appName)
                    .setIcon(icon).setIntent(shortcutIntent).build()
            shortcutManager.requestPinShortcut(shortcutInfo, null)
        } else {
            AlertDialog.Builder(context).setTitle(context.getText(R.string.error_creating_shortcut))
                .setMessage(context.getText(R.string.error_verbose_pin_shortcut)).setPositiveButton(
                    context.getText(android.R.string.ok)
                ) { dialog: DialogInterface, _: Int ->
                    // Just close dialog don't do anything
                    dialog.cancel()
                }.show()
        }
    }

    private fun createShortcutIntent(intent: Intent, asRoot: Boolean): Intent {
        val action = if(asRoot) {
            IconCreatorService.INTENT_LAUNCH_ROOT_SHORTCUT} else {IconCreatorService.INTENT_LAUNCH_SHORTCUT}
        val shortcutIntent = Intent(action)
        shortcutIntent.putExtra(IconCreatorService.INTENT_EXTRA_INTENT, intent.toUri(0))

        val signature: String
        try {
            signature = signingService.signIntent(intent)
            shortcutIntent.putExtra(IconCreatorService.INTENT_EXTRA_SIGNATURE, signature)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context, context.getText(R.string.error).toString() + ": " + e, Toast.LENGTH_LONG
            ).show()
        }

        return shortcutIntent
    }
}

