package de.szalkowski.activitylauncher.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    recentActivitiesService: RecentActivitiesService,
    activityListService: ActivityListService,
) : BaseActivityListViewModel(
    loadItems = {
        recentActivitiesService.getRecentActivities()
            .mapNotNull { recent ->
                runCatching { activityListService.getActivity(recent.componentName) }.getOrNull()
            }
    },
    onRemoveItem = { componentName ->
        recentActivitiesService.removeActivity(componentName)
    },
)
