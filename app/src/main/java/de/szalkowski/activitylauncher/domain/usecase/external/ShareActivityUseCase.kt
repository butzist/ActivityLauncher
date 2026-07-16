package de.szalkowski.activitylauncher.domain.usecase.external

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import javax.inject.Inject

class ShareActivityUseCase @Inject constructor(
    private val activitySharer: ActivitySharer,
    private val recentsRepository: RecentsRepository,
) {
    operator fun invoke(componentName: ComponentName) {
        activitySharer.shareActivity(componentName)
        recentsRepository.addActivity(componentName)
    }
}
