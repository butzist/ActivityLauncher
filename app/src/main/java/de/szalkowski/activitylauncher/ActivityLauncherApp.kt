package de.szalkowski.activitylauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.szalkowski.activitylauncher.services.SettingsService
import javax.inject.Inject

@HiltAndroidApp
class ActivityLauncherApp : Application() {
    @Inject
    internal lateinit var settingsService: SettingsService

    override fun onCreate() {
        super.onCreate()

        settingsService.init()
    }
}