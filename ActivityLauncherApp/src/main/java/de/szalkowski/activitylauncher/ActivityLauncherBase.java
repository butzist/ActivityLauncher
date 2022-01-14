package de.szalkowski.activitylauncher;

import android.app.Application;

import androidx.preference.PreferenceManager;

public class ActivityLauncherBase extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        var prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        Utils.setTheme(prefs.getString("theme","0"));

        if (!prefs.contains("allow_root")) {
            var hasSU = RootDetection.detectSU();
            prefs.edit().putBoolean("allow_root", hasSU).apply();
        }
        if (!prefs.contains("hide_private_activity")) {
            prefs.edit().putBoolean("hide_private_activity", true).apply();
        }

    }

}
