package de.szalkowski.activitylauncher.data.external

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import de.szalkowski.activitylauncher.domain.external.ReviewRequester
import javax.inject.Inject

class ReviewRequesterImpl @Inject constructor() : ReviewRequester {
    override fun showInAppReview(activity: Activity) {
        if (!shouldShowInAppReview(activity)) {
            return
        }

        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener {
                if (it.isSuccessful) {
                    runCatching {
                        val reviewInfo = it.result
                        manager.launchReviewFlow(activity, reviewInfo)
                    }
                }
            }
        }
    }

    private fun shouldShowInAppReview(context: Context): Boolean {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        val firstInstallTime = packageInfo.firstInstallTime

        val now = System.currentTimeMillis()
        val daysSinceInstall = (now - firstInstallTime) / 1000 / 60 / 60 / 24

        return daysSinceInstall > 20
    }
}
