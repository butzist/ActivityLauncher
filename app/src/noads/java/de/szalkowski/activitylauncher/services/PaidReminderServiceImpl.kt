package de.szalkowski.activitylauncher.services

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaidReminderServiceImpl @Inject constructor() : PaidReminderService {
    override fun shouldDisplayReminder(): Boolean = false
}
