package de.szalkowski.activitylauncher.domain.external

import android.app.Activity
import android.view.ViewGroup

interface AdManager {
    fun loadBanner(activity: Activity, container: ViewGroup)
    fun removeBanner(container: ViewGroup)
}
