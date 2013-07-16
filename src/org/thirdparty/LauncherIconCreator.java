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

import de.szalkowski.activitylauncher.MyActivityInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class LauncherIconCreator {
	final static private String LOG = "de.szalkowski.thirdparty.launchericoncreator";

	public static Intent getActivityIntent(ComponentName activity) {
		Intent intent = new Intent();
		intent.setComponent(activity);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		return intent;
	}

	public static void createLauncherIcon(Context context, MyActivityInfo activity) {
		createLauncherIcon(context, activity.componentName, activity.name, activity.icon_resource_name);	
	}

	public static void createLauncherIcon(Context context, ComponentName activity, String name, BitmapDrawable icon) {
	    Intent shortcutIntent = new Intent();
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getActivityIntent(activity));
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
	    Bitmap bm = icon.getBitmap();
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bm);
	    shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	    context.sendBroadcast(shortcutIntent);
	    //finish();
	}
	public static void createLauncherIcon(Context context, ComponentName activity, String name, String icon_resource_name) {
	    Intent shortcutIntent = new Intent();
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getActivityIntent(activity));
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
	    Intent.ShortcutIconResource ir = new Intent.ShortcutIconResource();
	    ir.packageName = activity.getPackageName();
	    ir.resourceName = icon_resource_name;
	    shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ir);
	    shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	    context.sendBroadcast(shortcutIntent);
	    //finish();
	}
}
