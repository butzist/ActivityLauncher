package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.view.ViewGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdServiceImpl @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) : AdService {
    override fun initialize(activity: Activity, container: ViewGroup) {}
}
