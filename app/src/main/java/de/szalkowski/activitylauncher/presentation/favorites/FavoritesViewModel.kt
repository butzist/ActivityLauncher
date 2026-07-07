package de.szalkowski.activitylauncher.presentation.favorites

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : BaseActivityListViewModel(
    getFlow = { favoritesRepository.getFavoritesFlow() },
    onRemoveItem = { request -> favoritesRepository.removeFavorite(request) },
)
