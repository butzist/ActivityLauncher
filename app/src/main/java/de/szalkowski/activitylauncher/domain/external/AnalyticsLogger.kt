package de.szalkowski.activitylauncher.domain.external

import androidx.navigation.NavDestination
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo

interface AnalyticsLogger {
    fun logDestination(destination: NavDestination?)
    fun logActivityAction(kind: String, activity: MyActivityInfo)
    fun logDisclaimerAccepted(accepted: Boolean)
    fun logSupportOption(option: String)
    fun logQsTileAction(action: String)
}
