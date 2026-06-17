package de.szalkowski.activitylauncher.domain.usecase.packages

import android.graphics.drawable.Drawable
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import javax.inject.Inject

class GetPackageIconUseCase @Inject constructor(
    private val iconLoader: IconLoader,
) {
    operator fun invoke(iconResourceName: String?, packageName: String): Drawable {
        if (iconResourceName != null) {
            val result = iconLoader.tryGetIcon(iconResourceName)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return iconLoader.getPackageIcon(packageName)
    }
}
