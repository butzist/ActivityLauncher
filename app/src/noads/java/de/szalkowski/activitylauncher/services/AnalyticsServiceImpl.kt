package de.szalkowski.activitylauncher.services

import androidx.navigation.NavDestination
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsServiceImpl @Inject constructor() : AnalyticsService {
    override fun logDestination(destination: NavDestination?) {}
    override fun logActivityAction(kind: String, activity: MyActivityInfo, asRoot: Boolean) {}
    override fun logDisclaimerAccepted(accepted: Boolean) {}
    override fun logSupportOption(option: String) {}
}
