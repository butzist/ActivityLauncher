package de.szalkowski.activitylauncher.domain.usecase.launcher

import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutResolutionResult
import de.szalkowski.activitylauncher.domain.model.ShortcutSource
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import javax.inject.Inject

open class ResolveShortcutSourceUseCase @Inject constructor(
    private val shortcutsRepository: ShortcutsRepository,
    private val intentSigner: IntentSigner,
    private val packageRepository: PackageRepository,
    private val getActivityIconUseCase: GetActivityIconUseCase,
) {
    open suspend operator fun invoke(source: ShortcutSource): ShortcutResolutionResult {
        if (source.id != null) {
            val managedShortcut = shortcutsRepository.getShortcut(source.id)
            if (managedShortcut != null) {
                return ShortcutResolutionResult.Success(managedShortcut.request.toLaunchRequest(LaunchSource.SHORTCUT))
            }
        }

        if ((source.intent != null) && (source.signature != null)) {
            val request = LaunchRequest(
                intent = source.intent,
                launcherPlugin = source.launcherPlugin,
                source = LaunchSource.SHORTCUT,
            )
            if (intentSigner.validateRequestSignature(request, source.signature)) {
                // Restoration logic
                val shortcutRequest = try {
                    request.toShortcutRequest(packageRepository, getActivityIconUseCase)
                } catch (_: Exception) {
                    null
                }

                if (shortcutRequest != null) {
                    shortcutsRepository.recordShortcut(shortcutRequest, source.id)
                }

                return ShortcutResolutionResult.Success(request)
            } else {
                return ShortcutResolutionResult.InvalidSignature
            }
        }

        return ShortcutResolutionResult.NotFound
    }
}
