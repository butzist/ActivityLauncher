package de.szalkowski.activitylauncher.presentation.favorites

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    favoritesRepository: FavoritesRepository,
    packageRepository: PackageRepository,
) : BaseActivityListViewModel(
    loadItems = {
        favoritesRepository.getFavorites()
            .mapNotNull { componentName ->
                runCatching { packageRepository.getActivity(componentName) }.getOrNull()
            }
    },
    onRemoveItem = { componentName ->
        favoritesRepository.removeFavorite(componentName)
    },
)
