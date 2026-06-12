package de.szalkowski.activitylauncher.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoritesService: FavoritesService,
    activityListService: ActivityListService,
) : BaseActivityListViewModel(
    loadItems = {
        favoritesService.getFavorites()
            .mapNotNull { componentName ->
                runCatching { activityListService.getActivity(componentName) }.getOrNull()
            }
    },
    onRemoveItem = { componentName ->
        favoritesService.removeFavorite(componentName)
    },
)
