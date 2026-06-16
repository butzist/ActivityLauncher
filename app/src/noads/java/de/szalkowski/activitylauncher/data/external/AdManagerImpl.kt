package de.szalkowski.activitylauncher.data.external

import android.app.Activity
import android.view.ViewGroup
import de.szalkowski.activitylauncher.domain.external.AdManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManagerImpl @Inject constructor() : AdManager {
    override fun loadBanner(activity: Activity, container: ViewGroup) {}
    override fun removeBanner(container: ViewGroup) {}
}
