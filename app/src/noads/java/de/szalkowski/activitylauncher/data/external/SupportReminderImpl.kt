package de.szalkowski.activitylauncher.data.external

import de.szalkowski.activitylauncher.domain.external.SupportReminder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportReminderImpl @Inject constructor() : SupportReminder {
    override fun shouldDisplayReminder(): Boolean = false
}
