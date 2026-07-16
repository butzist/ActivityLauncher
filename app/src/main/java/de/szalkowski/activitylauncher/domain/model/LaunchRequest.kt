package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase

data class LaunchRequest(
    val intent: Intent,
    val name: String? = null,
    val icon: IconCompat? = null,
    val launcherPlugin: ComponentName? = null,
) {
    fun toShortcutRequest(
        packageRepository: PackageRepository,
        getActivityIconUseCase: GetActivityIconUseCase,
    ): ShortcutRequest {
        val component = intent.component
            ?: throw IllegalArgumentException("Intent must have a component")

        val activityInfo = runCatching { packageRepository.getActivity(component) }.getOrNull()
        val resolvedName = name ?: activityInfo?.name ?: component.className.substringAfterLast('.')
        val resolvedIcon = icon ?: getActivityIconUseCase(activityInfo?.iconResourceName, component)

        return ShortcutRequest(
            name = resolvedName,
            intent = intent,
            icon = resolvedIcon,
            launcherPlugin = launcherPlugin,
        )
    }
}
