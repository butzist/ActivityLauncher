package de.szalkowski.activitylauncher.domain.usecase.favorites

import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) {
    operator fun invoke(request: ShortcutRequest): Boolean {
        val component = request.intent.component ?: return false
        return if (favoritesRepository.isFavorite(component)) {
            favoritesRepository.removeFavorite(request)
            false
        } else {
            favoritesRepository.addFavorite(request)
            true
        }
    }
}
