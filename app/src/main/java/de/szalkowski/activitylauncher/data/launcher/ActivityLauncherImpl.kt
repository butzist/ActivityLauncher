package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import javax.inject.Inject

class ActivityLauncherImpl @Inject constructor(@ApplicationContext private val context: Context) :
    ActivityLauncher {
    override fun launchActivity(
        activity: ComponentName,
        optionalExtras: Bundle?,
    ) {
        val intent = getActivityIntent(activity, optionalExtras)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getText(R.string.error).toString() + ": " + e,
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
