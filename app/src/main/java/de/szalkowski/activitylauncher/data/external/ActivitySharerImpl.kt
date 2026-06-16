package de.szalkowski.activitylauncher.data.external

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import javax.inject.Inject

class ActivitySharerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActivitySharer {
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
