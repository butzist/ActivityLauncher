package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.ViewGroup

interface AdService {
    fun loadBanner(activity: Activity, container: ViewGroup)
    fun removeBanner(container: ViewGroup)
}
