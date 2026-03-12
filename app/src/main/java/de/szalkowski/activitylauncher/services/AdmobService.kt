package de.szalkowski.activitylauncher.services

import android.app.Activity

interface AdmobService {
    fun initialize(activity: Activity)
    val isPrivacyOptionsRequired: Boolean
    fun showPrivacyOptionsDialog(activity: Activity)
    val isEnabled: Boolean
}
