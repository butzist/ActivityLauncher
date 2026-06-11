package de.szalkowski.activitylauncher

import dagger.hilt.android.HiltAndroidApp
import de.szalkowski.activitylauncher.services.SettingsService
import javax.inject.Inject

@HiltAndroidApp
class ActivityLauncherApp : ActivityLauncherBaseApp() {
    @Inject
    internal lateinit var settingsService: SettingsService

    override fun onCreate() {
        super.onCreate()

        settingsService.init()
    }
}
