package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class LaunchActivityUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityLauncher: ActivityLauncherProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(componentName: ComponentName, showToast: Boolean = true) {
        if (showToast) {
            Toast.makeText(
                context,
                String.format(
                    context.getText(R.string.starting_activity).toString(),
                    componentName.flattenToShortString(),
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
        activityLauncher.launchActivity(componentName)
        recentsRepository.addActivity(componentName)
    }
}
