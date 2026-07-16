package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import javax.inject.Inject

open class GetActivityIconUseCase @Inject constructor(
    private val iconLoader: IconLoader,
) {
    open operator fun invoke(iconResourceName: String?, componentName: ComponentName): ActivityIcon {
        if (iconResourceName != null) {
            val result = iconLoader.tryGetIcon(iconResourceName)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return iconLoader.getIcon(componentName)
    }
}
