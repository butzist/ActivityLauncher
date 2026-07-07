package de.szalkowski.activitylauncher.domain.recents

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.flow.Flow

interface RecentsRepository {
    data class RecentActivity(
        val componentName: ComponentName,
        val timestamp: Long,
    )

    fun getRecentActivities(): List<RecentActivity>
    fun getRecentsFlow(): Flow<List<ShortcutRequest>>
    fun addActivity(componentName: ComponentName)
    fun addActivity(request: ShortcutRequest)
    fun removeActivity(componentName: ComponentName)
    fun removeActivity(request: ShortcutRequest)
}
