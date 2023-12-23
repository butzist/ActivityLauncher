package de.szalkowski.activitylauncher

import android.app.Application
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import de.szalkowski.activitylauncher.todo.RootDetection
import de.szalkowski.activitylauncher.todo.SettingsUtils

@HiltAndroidApp
class ActivityLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(
            baseContext
        )

        SettingsUtils.setTheme(prefs.getString("theme", "0"))

        if (!prefs.contains("allow_root")) {
            val hasSU = RootDetection.detectSU()
            prefs.edit().putBoolean("allow_root", hasSU).apply()
        }

        if (!prefs.contains("hide_hide_private")) {
            prefs.edit().putBoolean("hide_hide_private", false).apply()
        }

        if (!prefs.contains("language")) {
            prefs.edit().putString("language", "System Default").apply()
        }
    }
}