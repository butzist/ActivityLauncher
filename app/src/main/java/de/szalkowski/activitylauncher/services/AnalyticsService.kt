package de.szalkowski.activitylauncher.services

import androidx.navigation.NavDestination

interface AnalyticsService {
    fun logDestination(destination: NavDestination?)
    fun logActivityAction(kind: String, activity: MyActivityInfo, asRoot: Boolean)
    fun logDisclaimerAccepted(accepted: Boolean)
    fun logSupportOption(option: String)
}
