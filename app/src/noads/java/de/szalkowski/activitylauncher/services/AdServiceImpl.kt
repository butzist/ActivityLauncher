package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdServiceImpl @Inject constructor() : AdService {
    override fun initialize(activity: Activity) {}
    override fun loadBanner(context: Context, container: ViewGroup) {}
    override fun destroyBanner() {}
}
