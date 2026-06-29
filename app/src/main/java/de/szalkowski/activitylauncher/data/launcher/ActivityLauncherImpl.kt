package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import javax.inject.Inject

class ActivityLauncherImpl @Inject constructor(@ApplicationContext private val context: Context) :
    ActivityLauncher {
    override fun launchActivity(request: LaunchRequest) {
        val intent = getActivityIntent(request.component, request.extras)
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
