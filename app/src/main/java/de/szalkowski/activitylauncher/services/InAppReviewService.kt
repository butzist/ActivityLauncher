package de.szalkowski.activitylauncher.services

import android.app.Activity
import javax.inject.Inject

interface InAppReviewService {
    fun showInAppReview(activity: Activity)
}

class InAppReviewServiceImplStub @Inject constructor() : InAppReviewService {
    override fun showInAppReview(activity: Activity) {}
}
