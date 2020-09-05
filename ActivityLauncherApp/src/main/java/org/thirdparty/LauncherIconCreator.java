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

package org.thirdparty;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;

import java.util.Objects;

import de.szalkowski.activitylauncher.MyActivityInfo;
import de.szalkowski.activitylauncher.MyPackageInfo;
import de.szalkowski.activitylauncher.R;

public class LauncherIconCreator {

    private static Intent getActivityIntent(ComponentName activity) {
        Intent intent = new Intent();
        intent.setComponent(activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    public static void createLauncherIcon(Context context, MyActivityInfo activity) {
        final String pack = activity.getIconResouceName().substring(0, activity.getIconResouceName().indexOf(':'));

        // Use bitmap version if icon from different package is used
        if (!pack.equals(activity.getComponentName().getPackageName())) {
            createShortcut(context, activity.getName(), activity.getIcon(), getActivityIntent(activity.getComponentName()), null);
        } else {
            createShortcut(context, activity.getName(), activity.getIcon(), getActivityIntent(activity.getComponentName()),
                    activity.getIconResouceName());
        }
    }

    public static boolean createLauncherIcon(Context context, MyPackageInfo pack) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pack.getPackageName());
        if (intent == null) {
            return false;
        }
        createShortcut(context, pack.getName(), pack.getIcon(), intent, pack.getIconResourceName());
        return true;
    }

    /**
     * Got reference from stackoverflow.com Url:
     * https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview?utm_medium=organic&utm_source=
     * google_rich_qa&utm_campaign=google_rich_qa
     */
    private static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    public static void launchActivity(Context context, ComponentName activity) {
        Intent intent = LauncherIconCreator.getActivityIntent(activity);
        Toast.makeText(context, String.format(context.getText(R.string.starting_activity).toString(), activity.flattenToShortString()),
                Toast.LENGTH_LONG).show();
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, context.getText(R.string.error).toString() + ": " + e.toString(), Toast.LENGTH_LONG).show();
        }

    }

    private static void createShortcut(Context context, String appName, Drawable draw, Intent intent, String iconResourceName) {
        Toast.makeText(context, String.format(context.getText(R.string.creating_application_shortcut).toString(), appName),
                Toast.LENGTH_LONG).show();

        if (Build.VERSION.SDK_INT >= 26) {
            doCreateShortcut(context, appName, draw, intent);
        } else {
            doCreateShortcut(context, appName, intent, iconResourceName);
        }
    }

    @TargetApi(14)
    private static void doCreateShortcut(Context context, String appName, Intent intent, String iconResourceName) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
        if (iconResourceName != null) {
            Intent.ShortcutIconResource ir = new Intent.ShortcutIconResource();
            if (intent.getComponent() == null) {
                ir.packageName = intent.getPackage();
            } else {
                ir.packageName = intent.getComponent().getPackageName();
            }
            ir.resourceName = iconResourceName;
            shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir);
        }
        shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        context.sendBroadcast(shortcutIntent);
    }

    @TargetApi(26)
    private static void doCreateShortcut(Context context, String appName, Drawable draw, Intent intent) {
        ShortcutManager shortcutManager = Objects.requireNonNull(context.getSystemService(ShortcutManager.class));

        if (shortcutManager.isRequestPinShortcutSupported()) {
            Bitmap bitmap = getBitmapFromDrawable(draw);
            intent.setAction(Intent.ACTION_CREATE_SHORTCUT);


            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, appName)
                    .setShortLabel(appName)
                    .setLongLabel(appName)
                    .setIcon(Icon.createWithBitmap(bitmap))
                    .setIntent(intent)
                    .build();

            shortcutManager.requestPinShortcut(shortcutInfo, null);
        } else {
            new AlertDialog.Builder(context)
                    .setTitle(context.getText(R.string.error_creating_shortcut))
                    .setMessage(context.getText(R.string.error_verbose_pin_shortcut))
                    .setPositiveButton(context.getText(android.R.string.ok), (dialog, which) -> {
                        // Just close dialog don't do anything
                        dialog.cancel();
                    })
                    .show();
        }
    }
}
