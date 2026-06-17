package de.szalkowski.activitylauncher.domain.recents

import android.content.ComponentName

interface RecentsRepository {
    data class RecentActivity(
        val componentName: ComponentName,
        val timestamp: Long,
    )

    fun getRecentActivities(): List<RecentActivity>
    fun addActivity(componentName: ComponentName)
    fun removeActivity(componentName: ComponentName)
}
