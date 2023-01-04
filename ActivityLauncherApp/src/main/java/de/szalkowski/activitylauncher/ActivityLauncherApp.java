package de.szalkowski.activitylauncher;

import android.app.Application;

import androidx.preference.PreferenceManager;

import de.szalkowski.activitylauncher.util.RootDetection;
import de.szalkowski.activitylauncher.util.SettingsUtils;
import de.szalkowski.activitylauncher.constant.Constants;

public class ActivityLauncherApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        var prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SettingsUtils.setTheme(prefs.getString(Constants.PREF_THEME, "0"));

        if (!prefs.contains(Constants.PREF_ALLOW_ROOT)) {
            var hasSU = RootDetection.detectSU();
            prefs.edit().putBoolean(Constants.PREF_ALLOW_ROOT, hasSU).apply();
        }

        if (!prefs.contains(Constants.PREF_HIDE_HIDE_PRIVATE)) {
            prefs.edit().putBoolean(Constants.PREF_HIDE_HIDE_PRIVATE, false).apply();
        }

        if (!prefs.contains(Constants.PREF_LANGUAGE)) {
            prefs.edit().putString(Constants.PREF_LANGUAGE, "System Default").apply();
        }
    }
}
