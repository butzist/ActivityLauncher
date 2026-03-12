package de.szalkowski.activitylauncher.services

import android.app.Activity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdmobServiceImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) : AdmobService {
    override fun initialize(activity: Activity) {}
    override fun showPrivacyOptionsDialog(activity: Activity) {}
    override val isPrivacyOptionsRequired: Boolean
        get() = false
    override val isEnabled: Boolean
        get() = false
}
