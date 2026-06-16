package de.szalkowski.activitylauncher.presentation.favorites

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoritesRepository: FavoritesRepository,
    activityRepository: ActivityRepository,
) : BaseActivityListViewModel(
    loadItems = {
        favoritesRepository.getFavorites()
            .mapNotNull { componentName ->
                runCatching { activityRepository.getActivity(componentName) }.getOrNull()
            }
    },
    onRemoveItem = { componentName ->
        favoritesRepository.removeFavorite(componentName)
    },
)
