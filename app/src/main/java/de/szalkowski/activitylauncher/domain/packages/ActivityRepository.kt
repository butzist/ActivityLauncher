package de.szalkowski.activitylauncher.domain.packages

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PackageActivities

interface ActivityRepository {
    fun getActivities(
        packageName: String,
    ): PackageActivities

    fun getActivity(
        componentName: ComponentName,
    ): MyActivityInfo

    fun getIcon(componentName: ComponentName): android.graphics.drawable.Drawable

    fun invalidate()
}
