package de.szalkowski.activitylauncher.presentation.favorites

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : BaseActivityListViewModel<ShortcutRequest>(
    getFlow = { favoritesRepository.getFavoritesFlow() },
    onRemoveItem = { request -> favoritesRepository.removeFavorite(request) },
) {
    override fun handleSaveResult(result: DetailsResult) {
        when (result) {
            is DetailsResult.Save -> {
                favoritesRepository.addFavorite(result.request)
            }
        }
    }
}
