package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.ViewGroup

interface AdService {
    fun initialize(
        activity: Activity,
        container: ViewGroup,
    )
}
