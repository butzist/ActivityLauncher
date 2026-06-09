package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.ViewGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdServiceImpl @Inject constructor() : AdService {
    override fun loadBanner(activity: Activity, container: ViewGroup) {}
    override fun removeBanner(container: ViewGroup) {}
}
