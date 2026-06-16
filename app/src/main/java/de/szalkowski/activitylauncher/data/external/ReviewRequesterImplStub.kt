package de.szalkowski.activitylauncher.data.external

import android.app.Activity
import de.szalkowski.activitylauncher.domain.external.ReviewRequester
import javax.inject.Inject

class ReviewRequesterImplStub @Inject constructor() : ReviewRequester {
    override fun showInAppReview(activity: Activity) {}
}
