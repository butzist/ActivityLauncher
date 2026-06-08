package de.szalkowski.activitylauncher.services

import android.app.Activity
import android.content.Context
import android.view.ViewGroup

interface AdService {
    fun initialize(activity: Activity)
    fun loadBanner(context: Context, container: ViewGroup)
    fun destroyBanner()
}
