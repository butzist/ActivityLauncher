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
        // Sharing also adds to recents? Maybe. Let's assume yes for consistent testing if desired,
        // or just keep it simple if we only want to proxy it.
        // For now, let's just proxy it to match current behavior but in a use case.
    }
}
