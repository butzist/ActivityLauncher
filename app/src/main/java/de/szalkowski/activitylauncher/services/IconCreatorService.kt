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
import java.util.Objects
import javax.inject.Inject

private const val INTENT_LAUNCH_SHORTCUT = "activitylauncher.intent.action.LAUNCH_SHORTCUT"

interface IconCreatorService {
    fun createLauncherIcon(activity: MyActivityInfo, extras: Bundle?)
    fun createLauncherIcon(activity: MyActivityInfo)
    fun createLauncherIcon(pack: MyPackageInfo)
}

class IconCreatorServiceImpl @Inject constructor(@ActivityContext private val context: Context) :
    IconCreatorService {
    override fun createLauncherIcon(activity: MyActivityInfo, extras: Bundle?) {
        val pack = extractIconPackageName(activity)
        val name: String = activity.name
        val intent = getActivityIntent(activity.componentName, extras)
        val icon: Drawable = activity.icon

        // Use bitmap version, if icon from different package is used
        if (pack != null && pack != activity.componentName.packageName) {
            createShortcut(name, icon, intent, null)
        } else {
            createShortcut(name, icon, intent, activity.iconResourceName)
        }
    }

    override fun createLauncherIcon(activity: MyActivityInfo) {
        createLauncherIcon(activity, null)
    }

    override fun createLauncherIcon(pack: MyPackageInfo) {
        val intent = context.packageManager.getLaunchIntentForPackage(pack.packageName) ?: return
        createShortcut(pack.name, pack.icon, intent, pack.iconResourceName)
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
        appName: String, draw: Drawable, intent: Intent, iconResourceName: String?
    ) {
        Toast.makeText(
            context, String.format(
                context.getText(R.string.creating_application_shortcut).toString(), appName
            ), Toast.LENGTH_LONG
        ).show()
        if (Build.VERSION.SDK_INT >= 26) {
            doCreateShortcut(appName, draw, intent)
        } else {
            doCreateShortcut(appName, intent, iconResourceName)
        }
    }

    private fun doCreateShortcut(
        appName: String, intent: Intent, iconResourceName: String?
    ) {
        val shortcutIntent = Intent()
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
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
        appName: String, draw: Drawable, extraIntent: Intent
    ) {
        val shortcutManager = Objects.requireNonNull(
            context.getSystemService(
                ShortcutManager::class.java
            )
        )
        if (shortcutManager.isRequestPinShortcutSupported) {
            val icon = getIconFromDrawable(draw)
            val intent = Intent(INTENT_LAUNCH_SHORTCUT)
            intent.putExtra("extra_intent", extraIntent.toUri(0))
            val shortcutInfo =
                ShortcutInfo.Builder(context, appName).setShortLabel(appName).setLongLabel(appName)
                    .setIcon(icon).setIntent(intent).build()
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
}

