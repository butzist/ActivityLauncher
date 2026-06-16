package de.szalkowski.activitylauncher.domain.usecase.external

import de.szalkowski.activitylauncher.domain.external.SupportReminder
import javax.inject.Inject

class CalculateSupportReminderUseCase @Inject constructor(
    private val supportReminder: SupportReminder,
) {
    operator fun invoke(): Boolean {
        return supportReminder.shouldDisplayReminder()
    }
}
