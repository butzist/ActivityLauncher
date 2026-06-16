package de.szalkowski.activitylauncher.domain.recents

import android.content.ComponentName

interface RecentsRepository {
    data class RecentActivity(
        val componentName: ComponentName,
        val wasRoot: Boolean,
        val timestamp: Long,
    )

    fun getRecentActivities(): List<RecentActivity>
    fun addActivity(componentName: ComponentName, wasRoot: Boolean)
    fun removeActivity(componentName: ComponentName)
}
