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

        if (!prefs.contains("hide_private_activities")) {
            prefs.edit().putBoolean("hide_private_activities", true).apply();
        }
        if (!prefs.contains("locale")) {
            prefs.edit().putString("locale", "System Default").apply();
        }
    }
}
