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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Objects;


import de.szalkowski.activitylauncher.MyActivityInfo;
import de.szalkowski.activitylauncher.MyPackageInfo;
import de.szalkowski.activitylauncher.R;

public class IconCreator {

    private static String INTENT_LAUNCH_SHORTCUT = "activitylauncher.intent.action.LAUNCH_SHORTCUT";

    public static Intent getActivityIntent(ComponentName activity, Bundle extras) {
        Intent intent = new Intent();
        intent.setComponent(activity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (extras != null) {
            intent.putExtras(extras);
        }

        return intent;
    }

    public static void createLauncherIcon(Context context, MyActivityInfo activity, Bundle extras) {
        String pack = null;

        if (activity.getIconResouceName() != null && activity.getIconResouceName().indexOf(':') >= 0) {
            pack = activity.getIconResouceName().substring(0, activity.getIconResouceName().indexOf(':'));
        }

        String name = activity.getName();
        Intent intent = getActivityIntent(activity.getComponentName(), extras);
        Drawable icon = activity.getIcon();


        // Use bitmap version if icon from different package is used
        if (pack != null && !pack.equals(activity.getComponentName().getPackageName())) {
            createShortcut(context, name, icon, intent, null);
        } else {
            createShortcut(context, name, icon, intent, activity.getIconResouceName());
        }
    }

    public static void createLauncherIcon(Context context, MyActivityInfo activity) {
        createLauncherIcon(context, activity, null);
    }

    public static void createLauncherIcon(Context context, MyPackageInfo pack) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pack.getPackageName());
        if (intent == null) {
            return;
        }
        createShortcut(context, pack.getName(), pack.getIcon(), intent, pack.getIconResourceName());
    }

    /**
     * Got reference from stackoverflow.com URL:
     * https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview
     * https://stackoverflow.com/questions/46130594/android-get-apps-adaptive-icons-from-package-manager
     */
    @TargetApi(26)
    private static Icon getIconFromDrawable(Drawable drawable) {
        if (drawable instanceof AdaptiveIconDrawable) {
            Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
            Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

            Drawable[] drr = new Drawable[2];
            drr[0] = backgroundDr;
            drr[1] = foregroundDr;

            LayerDrawable layerDrawable = new LayerDrawable(drr);

            int width = layerDrawable.getIntrinsicWidth();
            int height = layerDrawable.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            layerDrawable.draw(canvas);

            return Icon.createWithAdaptiveBitmap(bitmap);
        }
        if (drawable instanceof BitmapDrawable) {
            return Icon.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
        }

        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return Icon.createWithBitmap(bmp);
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
    private static void doCreateShortcut(Context context, String appName, Drawable draw, Intent extraIntent) {
        ShortcutManager shortcutManager = Objects.requireNonNull(context.getSystemService(ShortcutManager.class));

        if (shortcutManager.isRequestPinShortcutSupported()) {
            Icon icon = getIconFromDrawable(draw);
            Intent intent = new Intent(INTENT_LAUNCH_SHORTCUT);
            intent.putExtra("extra_intent", extraIntent.toUri(0));

            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, appName)
                    .setShortLabel(appName)
                    .setLongLabel(appName)
                    .setIcon(icon)
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
