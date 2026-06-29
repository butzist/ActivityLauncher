package de.szalkowski.activitylauncher.domain.usecase.packages

import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import javax.inject.Inject

open class GetPackageIconUseCase @Inject constructor(
    private val iconLoader: IconLoader,
) {
    open operator fun invoke(iconResourceName: String?, packageName: String): IconCompat {
        if (iconResourceName != null) {
            val result = iconLoader.tryGetIcon(iconResourceName)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return iconLoader.getPackageIcon(packageName)
    }
}
