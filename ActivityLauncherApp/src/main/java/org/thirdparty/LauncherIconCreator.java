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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.szalkowski.activitylauncher.MyActivityInfo;
import de.szalkowski.activitylauncher.MyPackageInfo;
import de.szalkowski.activitylauncher.R;

public class LauncherIconCreator {

    private static Intent getActivityIntent(ComponentName activity, Bundle extras) {
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

    /**
     * Got reference from stackoverflow.com URL
     * https://stackoverflow.com/questions/9194725/run-android-program-as-root
     * https://stackoverflow.com/questions/12343227/escaping-bash-function-arguments-for-use-by-su-c
     */
    public static void launchActivity(Context context, ComponentName activity, boolean asRoot) {
        Intent intent = LauncherIconCreator.getActivityIntent(activity, null);
        Toast.makeText(context, String.format(context.getText(R.string.starting_activity).toString(), activity.flattenToShortString()),
                Toast.LENGTH_LONG).show();

        try {
            if (!asRoot) {
                context.startActivity(intent);
            } else {
                startRootActivity(context, activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getText(R.string.error).toString() + ": " + e, Toast.LENGTH_LONG).show();
        }
    }

    private static void startRootActivity(Context context, ComponentName activity) throws IOException, InterruptedException, IllegalArgumentException {
        var component = activity.flattenToShortString();
        boolean isValid = validateComponentName(component);
        if (!isValid) {
            throw new IllegalArgumentException(String.format(context.getString(R.string.exception_invalid_component_name), component));
        }
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "am start -n " + component});
        String output = getProcessOutput(process);

        var exitValue = process.waitFor();
        if (exitValue > 0) {
            throw new RuntimeException(String.format(context.getString(R.string.exception_command_error), exitValue, output));
        }
    }

    /**
     * Got reference from stackoverflow.com URL:
     * https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
     */
    @NonNull
    private static String getProcessOutput(Process process) throws IOException {
        var stream = process.getErrorStream();
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

    /**
     * In order to be on the safe side, validate component name before merging it into a root shell command
     *
     * @param component component name
     * @return true, if valid
     */
    private static boolean validateComponentName(String component) {
        Pattern p = Pattern.compile("^[./a-zA-Z0-9]+$");
        Matcher m = p.matcher(component);
        return m.matches();
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

    @TargetApi(19)
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
            Icon icon = getIconFromDrawable(draw);
            intent.setAction(Intent.ACTION_CREATE_SHORTCUT);

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
