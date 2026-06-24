package de.szalkowski.activitylauncher.presentation.recents

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    recentsRepository: RecentsRepository,
    packageRepository: PackageRepository,
) : BaseActivityListViewModel(
    loadItems = {
        recentsRepository.getRecentActivities()
            .mapNotNull { recent ->
                runCatching { packageRepository.getActivity(recent.componentName) }.getOrNull()
            }
    },
    onRemoveItem = { componentName ->
        recentsRepository.removeActivity(componentName)
    },
)
