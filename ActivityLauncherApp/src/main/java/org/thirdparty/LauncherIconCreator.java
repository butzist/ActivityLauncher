/**
 * Based on code from Stackoverflow.com under CC BY-SA 3.0
 * Url: http://stackoverflow.com/questions/6493518/create-a-shortcut-for-any-app-on-desktop
 * By:  http://stackoverflow.com/users/815400/xuso
 * 
 * and
 * 
 * Url: http://stackoverflow.com/questions/3298908/creating-a-shortcut-how-can-i-work-with-a-drawable-as-icon
 * By:  http://stackoverflow.com/users/327402/waza-be
 */

package org.thirdparty;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import de.szalkowski.activitylauncher.MyActivityInfo;
import de.szalkowski.activitylauncher.MyPackageInfo;
import de.szalkowski.activitylauncher.R;

public class LauncherIconCreator {

	public static Intent getActivityIntent(ComponentName activity) {
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

	public static void createLauncherIcon(Context context, MyPackageInfo pack) {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(pack.getPackageName());
		// createLauncherIcon(context, intent, pack.getName(), pack.getIconResourceName(), pack.getIcon());
		createShortcut(context, pack.getName(), pack.getIcon(), intent, pack.getIconResourceName());
	}

	/**
	 * Got reference from stackoverflow.com Url:
	 * https://stackoverflow.com/questions/44447056/convert-adaptiveicondrawable-to-bitmap-in-android-o-preview?utm_medium=organic&utm_source=
	 * google_rich_qa&utm_campaign=google_rich_qa
	 * 
	 * @param drawable
	 * @return
	 */
	private static Bitmap getBitmapFromDrawable(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}
		final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(bmp);
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

	@TargetApi(26)
	@SuppressWarnings("deprecation")
	public static void createShortcut(Context context, String appName, Drawable draw, Intent intent, String icon_resource_name) {
		// Handling for Android Oreo and thereafter
		if (Build.VERSION.SDK_INT >= 26) {
			ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);
			if (mShortcutManager.isRequestPinShortcutSupported()) {
				ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(context, appName);
				mShortcutInfoBuilder.setShortLabel(appName);
				mShortcutInfoBuilder.setLongLabel(appName);
				Bitmap bitmap = getBitmapFromDrawable(draw);
				mShortcutInfoBuilder.setIcon(Icon.createWithBitmap(bitmap));
				intent.setAction(Intent.ACTION_CREATE_SHORTCUT);
				mShortcutInfoBuilder.setIntent(intent);
				ShortcutInfo mShortcutInfo = mShortcutInfoBuilder.build();
				boolean result = mShortcutManager.requestPinShortcut(mShortcutInfo, null);
				if (result) {
					Toast.makeText(context, String.format(context.getText(R.string.creating_application_shortcut).toString(), appName),
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(context, "Failed creating shortcut", Toast.LENGTH_LONG).show();
				}
			} else {
				AlertDialog.Builder alertBuild = new AlertDialog.Builder(context);
				alertBuild.setTitle("Error creating shortcut")
						.setMessage("Current Launcher does not support Pin Shortcuts. Unable to create shortcuts")
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Just close dialog don't do anything
								dialog.cancel();
							}
						});
				alertBuild.show();
				Toast.makeText(context, "Failed creating shortcut", Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(context, String.format(context.getText(R.string.creating_application_shortcut).toString(), appName), Toast.LENGTH_LONG)
					.show();
			Intent shortcutIntent = new Intent();
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
			if (icon_resource_name != null) {
				Intent.ShortcutIconResource ir = new Intent.ShortcutIconResource();
				if (intent.getComponent() == null) {
					ir.packageName = intent.getPackage();
				} else {
					ir.packageName = intent.getComponent().getPackageName();
				}
				ir.resourceName = icon_resource_name;
				shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir);
			}
			shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			context.sendBroadcast(shortcutIntent);
		}
	}
}
