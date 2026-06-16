package de.szalkowski.activitylauncher.data.recents

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : RecentsRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("al_recent_activities", Context.MODE_PRIVATE)
    private val recentsKey = "recents"
    private val maxRecents = 20

    override fun getRecentActivities(): List<RecentsRepository.RecentActivity> {
        val strings = prefs.getStringSet(recentsKey, emptySet()) ?: emptySet()
        return strings.mapNotNull { fromString(it) }
            .sortedByDescending { it.timestamp }
    }

    override fun addActivity(componentName: ComponentName, wasRoot: Boolean) {
        val recents = getRecentActivities().toMutableList()
        // Remove if already exists to update its timestamp and wasRoot status
        recents.removeAll { it.componentName == componentName }
        recents.add(0, RecentsRepository.RecentActivity(componentName, wasRoot, System.currentTimeMillis()))

        val newRecents = recents.take(maxRecents)

        val newStringSet = newRecents.map { toString(it) }.toSet()
        prefs.edit().putStringSet(recentsKey, newStringSet).apply()
    }

    override fun removeActivity(componentName: ComponentName) {
        val recents = getRecentActivities().toMutableList()
        recents.removeAll { it.componentName == componentName }
        val newStringSet = recents.map { toString(it) }.toSet()
        prefs.edit().putStringSet(recentsKey, newStringSet).apply()
    }

    private fun toString(activity: RecentsRepository.RecentActivity): String {
        return "${activity.componentName.flattenToString()};${activity.wasRoot};${activity.timestamp}"
    }

    private fun fromString(string: String): RecentsRepository.RecentActivity? {
        return try {
            val parts = string.split(';')
            val componentName = ComponentName.unflattenFromString(parts[0])!!
            val wasRoot = parts[1].toBoolean()
            val timestamp = parts[2].toLong()
            RecentsRepository.RecentActivity(componentName, wasRoot, timestamp)
        } catch (e: Exception) {
            null
        }
    }
}
