package de.szalkowski.activitylauncher.domain.external

import android.app.Activity

interface ReviewRequester {
    fun showInAppReview(activity: Activity)
}
