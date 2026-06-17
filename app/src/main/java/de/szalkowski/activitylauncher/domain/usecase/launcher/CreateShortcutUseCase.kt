package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class CreateShortcutUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shortcutCreator: ShortcutCreatorProxy,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(activity: MyActivityInfo, optionalExtras: Bundle? = null, showToast: Boolean = true) {
        if (showToast) {
            Toast.makeText(
                context,
                String.format(
                    context.getText(R.string.creating_application_shortcut).toString(),
                    activity.name,
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
        shortcutCreator.createLauncherIcon(activity, null, optionalExtras)
        recentsRepository.addActivity(activity.componentName)
    }
}
