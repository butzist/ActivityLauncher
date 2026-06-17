package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import javax.inject.Inject

open class GetActivityIconUseCase @Inject constructor(
    private val iconLoader: IconLoader,
) {
    operator fun invoke(iconResourceName: String?, componentName: ComponentName): IconCompat {
        if (iconResourceName != null) {
            val result = iconLoader.tryGetIcon(iconResourceName)
            if (result.isSuccess) {
                return result.getOrThrow()
            }
        }
        return iconLoader.getIcon(componentName)
    }
}
