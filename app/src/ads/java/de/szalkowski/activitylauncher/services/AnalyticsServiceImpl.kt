package de.szalkowski.activitylauncher.services

import android.content.Context
import android.os.Bundle
import androidx.navigation.NavDestination
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnalyticsService {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logDestination(destination: NavDestination?) {
        runCatching {
            val screenName = when (destination?.id) {
                de.szalkowski.activitylauncher.R.id.LoadingFragment -> "Loading"
                de.szalkowski.activitylauncher.R.id.PackageListFragment -> "PackageList"
                de.szalkowski.activitylauncher.R.id.ActivityListFragment -> "ActivityList"
                de.szalkowski.activitylauncher.R.id.ActivityDetailsFragment -> "Details"
                else -> destination?.label?.toString() ?: destination?.id?.toString() ?: "Unknown"
            }

            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }

    override fun logActivityAction(kind: String, activity: MyActivityInfo, asRoot: Boolean) {
        runCatching {
            val bundle = Bundle().apply {
                putString("action_type", kind)
                putString("package_name", activity.componentName.packageName)
                putString("activity_name", activity.componentName.className)
                putString("launch_mode", if (asRoot) "root" else "normal")
            }

            firebaseAnalytics.logEvent("activity_action", bundle)
        }
    }

    override fun logDisclaimerAccepted(accepted: Boolean) {
        runCatching {
            val bundle = Bundle().apply {
                putBoolean("accepted", accepted)
            }

            firebaseAnalytics.logEvent("disclaimer_accepted", bundle)
        }
    }

    override fun logSupportOption(option: String) {
        runCatching {
            val bundle = Bundle().apply {
                putString("option", option)
            }

            firebaseAnalytics.logEvent("support_option", bundle)
        }
    }
}
