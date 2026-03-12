package de.szalkowski.activitylauncher.services

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaidReminderServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PaidReminderService {
    companion object {
        private const val PREFERENCES_NAME = "paid_reminder"
        private const val PREF_LAST_DISPLAY_TIMESTAMP = "last_display_timestamp"
        private const val PREF_NEXT_DISPLAY_DELAY_DAYS = "last_display_delay_days"
    }

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun shouldDisplayReminder(): Boolean {
        val currentTime = System.currentTimeMillis()

        val lastTime = prefs.getLong(PREF_LAST_DISPLAY_TIMESTAMP, 0L)
        val nextDisplayDelayDays = prefs.getLong(PREF_NEXT_DISPLAY_DELAY_DAYS, 0L)

        val shouldDisplay =
            currentTime > lastTime + (nextDisplayDelayDays * 24 * 30 * 60 * 60 * 1000)
        if (shouldDisplay) {
            val newDisplayDelayDays = minOf(maxOf(nextDisplayDelayDays * 2, 30), 365)

            prefs.edit(commit = true) {
                putLong(PREF_LAST_DISPLAY_TIMESTAMP, currentTime)
                putLong(PREF_NEXT_DISPLAY_DELAY_DAYS, newDisplayDelayDays)
            }
        }

        return shouldDisplay
    }
}
