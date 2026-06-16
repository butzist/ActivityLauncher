package de.szalkowski.activitylauncher.domain.usecase.favorites

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) {
    operator fun invoke(componentName: ComponentName) {
        if (favoritesRepository.isFavorite(componentName)) {
            favoritesRepository.removeFavorite(componentName)
        } else {
            favoritesRepository.addFavorite(componentName)
        }
    }
}
