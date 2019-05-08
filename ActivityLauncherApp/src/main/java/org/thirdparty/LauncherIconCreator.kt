/*
 * Based on code from Stackoverflow.com under CC BY-SA 3.0
 * Url: http://stackoverflow.com/questions/6493518/create-a-shortcut-for-any-app-on-desktop
 * By:  http://stackoverflow.com/users/815400/xuso
 * <p>
 * and
 * <p>
 * Url: http://stackoverflow.com/questions/3298908/creating-a-shortcut-how-can-i-work-with-a-drawable-as-icon
 * By:  http://stackoverflow.com/users/327402/waza-be
 */

package org.thirdparty

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast

import java.util.Objects

import de.szalkowski.activitylauncher.MyActivityInfo
import de.szalkowski.activitylauncher.MyPackageInfo
import de.szalkowski.activitylauncher.R

object LauncherIconCreator {

    private fun getActivityIntent(activity: ComponentName): Intent {
        val intent = Intent()
        intent.component = activity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return intent
    }

    fun createLauncherIcon(context: Context, activity: MyActivityInfo) {
        val pack = activity.iconResouceName!!.substring(0, activity.iconResouceName!!.indexOf(':'))

        // Use bitmap version if icon from different package is used
        if (pack != activity.componentName.packageName) {
            createShortcut(context, activity.name, activity.icon, getActivityIntent(activity.componentName), null)
        } else {
            createShortcut(context, activity.name, activity.icon, getActivityIntent(activity.componentName),
                    activity.iconResouceName)
        }
    }

    fun createLauncherIcon(context: Context, pack: MyPackageInfo) {
        val intent = context.packageManager.getLaunchIntentForPackage(pack.packageName)
        createShortcut(context, pack.name, pack.icon, intent, pack.iconResourceName)
    }

    /**
     * Got reference from stackoverflow.com Url:
     * https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview?utm_medium=organic&utm_source=
     * google_rich_qa&utm_campaign=google_rich_qa
     */
    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    fun launchActivity(context: Context, activity: ComponentName) {
        val intent = LauncherIconCreator.getActivityIntent(activity)
        Toast.makeText(context, String.format(context.getText(R.string.starting_activity).toString(), activity.flattenToShortString()),
                Toast.LENGTH_LONG).show()
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getText(R.string.error).toString() + ": " + e.toString(), Toast.LENGTH_LONG).show()
        }

    }

    private fun createShortcut(context: Context, appName: String, draw: Drawable, intent: Intent?, iconResourceName: String?) {
        Toast.makeText(context, String.format(context.getText(R.string.creating_application_shortcut).toString(), appName),
                Toast.LENGTH_LONG).show()

        if (Build.VERSION.SDK_INT >= 26) {
            doCreateShortcut(context, appName, draw, intent)
        } else {
            doCreateShortcut(context, appName, intent, iconResourceName)
        }
    }

    @TargetApi(14)
    private fun doCreateShortcut(context: Context, appName: String, intent: Intent?, iconResourceName: String?) {
        val shortcutIntent = Intent()
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName)
        if (iconResourceName != null) {
            val ir = Intent.ShortcutIconResource()
            if (intent!!.component == null) {
                ir.packageName = intent.getPackage()
            } else {
                ir.packageName = intent.component!!.packageName
            }
            ir.resourceName = iconResourceName
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir)
        }
        shortcutIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
        context.sendBroadcast(shortcutIntent)
    }

    @TargetApi(26)
    private fun doCreateShortcut(context: Context, appName: String, draw: Drawable, intent: Intent?) {
        val shortcutManager = Objects.requireNonNull(context.getSystemService<ShortcutManager>(ShortcutManager::class.java))

        if (shortcutManager.isRequestPinShortcutSupported) {
            val bitmap = getBitmapFromDrawable(draw)
            intent!!.action = Intent.ACTION_CREATE_SHORTCUT


            val shortcutInfo = ShortcutInfo.Builder(context, appName)
                    .setShortLabel(appName)
                    .setLongLabel(appName)
                    .setIcon(Icon.createWithBitmap(bitmap))
                    .setIntent(intent)
                    .build()

            shortcutManager.requestPinShortcut(shortcutInfo, null)
        } else {
            AlertDialog.Builder(context)
                    .setTitle(context.getText(R.string.error_creating_shortcut))
                    .setMessage(context.getText(R.string.error_verbose_pin_shortcut))
                    .setPositiveButton(context.getText(android.R.string.ok)) { dialog, which ->
                        // Just close dialog don't do anything
                        dialog.cancel()
                    }
                    .show()
        }
    }
}
