package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentActivitiesService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("al_recent_activities", Context.MODE_PRIVATE)
    private val recentsKey = "recents"
    private val maxRecents = 20

    data class RecentActivity(
        val componentName: ComponentName,
        val wasRoot: Boolean,
        val timestamp: Long
    )

    fun getRecentActivities(): List<RecentActivity> {
        val strings = prefs.getStringSet(recentsKey, emptySet()) ?: emptySet()
        return strings.mapNotNull { fromString(it) }
            .sortedByDescending { it.timestamp }
    }

    fun addActivity(componentName: ComponentName, wasRoot: Boolean) {
        val recents = getRecentActivities().toMutableList()
        // Remove if already exists to update its timestamp and wasRoot status
        recents.removeAll { it.componentName == componentName }
        recents.add(0, RecentActivity(componentName, wasRoot, System.currentTimeMillis()))

        val newRecents = recents.take(maxRecents)

        val newStringSet = newRecents.map { toString(it) }.toSet()
        prefs.edit().putStringSet(recentsKey, newStringSet).apply()
    }

    private fun toString(activity: RecentActivity): String {
        return "${activity.componentName.flattenToString()};${activity.wasRoot};${activity.timestamp}"
    }

    private fun fromString(string: String): RecentActivity? {
        return try {
            val parts = string.split(';')
            val componentName = ComponentName.unflattenFromString(parts[0])!!
            val wasRoot = parts[1].toBoolean()
            val timestamp = parts[2].toLong()
            RecentActivity(componentName, wasRoot, timestamp)
        } catch (e: Exception) {
            null
        }
    }
}
