package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface ShareActivityService {
    fun shareActivity(activity: ComponentName)
}

class ShareActivityServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShareActivityService {
    override fun shareActivity(activity: ComponentName) {
        val url = "https://activitylauncher.net/activity/${activity.flattenToShortString()}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(shareIntent, "Share link via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}