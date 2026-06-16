package de.szalkowski.activitylauncher.app

import dagger.hilt.android.HiltAndroidApp
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import javax.inject.Inject

@HiltAndroidApp
class ActivityLauncherApp : ActivityLauncherBaseApp() {
    @Inject
    internal lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()

        settingsRepository.init()
    }
}
