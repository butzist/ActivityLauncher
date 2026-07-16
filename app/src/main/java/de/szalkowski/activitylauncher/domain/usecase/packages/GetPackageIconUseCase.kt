package de.szalkowski.activitylauncher.domain.usecase.packages

import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import javax.inject.Inject

open class GetPackageIconUseCase @Inject constructor(
    private val iconLoader: IconLoader,
) {
    open operator fun invoke(iconResourceName: String?, packageName: String): ActivityIcon {
        if (iconResourceName != null) {
            val result = iconLoader.tryGetIcon(iconResourceName)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return iconLoader.getPackageIcon(packageName)
    }
}
