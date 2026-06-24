package de.szalkowski.activitylauncher.domain.external

import androidx.navigation.NavDestination
import de.szalkowski.activitylauncher.domain.model.SystemActivity

interface AnalyticsLogger {
    fun logDestination(destination: NavDestination?)
    fun logActivityAction(kind: String, activity: SystemActivity)
    fun logDisclaimerAccepted(accepted: Boolean)
    fun logSupportOption(option: String)
    fun logQsTileAction(action: String)
}
