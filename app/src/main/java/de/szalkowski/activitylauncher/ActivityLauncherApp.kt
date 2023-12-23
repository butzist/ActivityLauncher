package de.szalkowski.activitylauncher;

import android.app.Application;

import androidx.preference.PreferenceManager;

public class ActivityLauncherApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        var prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        SettingsUtils.setTheme(prefs.getString("theme", "0"));

        if (!prefs.contains("allow_root")) {
            var hasSU = RootDetection.detectSU();
            prefs.edit().putBoolean("allow_root", hasSU).apply();
        }

        if (!prefs.contains("hide_hide_private")) {
            prefs.edit().putBoolean("hide_hide_private", false).apply();
        }

        if (!prefs.contains("language")) {
            prefs.edit().putString("language", "System Default").apply();
        }
    }
}
