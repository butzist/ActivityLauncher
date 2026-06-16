package de.szalkowski.activitylauncher.data.external

import androidx.navigation.NavDestination
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsLoggerImpl @Inject constructor() : AnalyticsLogger {
    override fun logDestination(destination: NavDestination?) {}
    override fun logActivityAction(kind: String, activity: MyActivityInfo, asRoot: Boolean) {}
    override fun logDisclaimerAccepted(accepted: Boolean) {}
    override fun logSupportOption(option: String) {}
    override fun logQsTileAction(action: String) {}
}
