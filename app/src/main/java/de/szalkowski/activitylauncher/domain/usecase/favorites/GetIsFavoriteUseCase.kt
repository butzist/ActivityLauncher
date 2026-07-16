package de.szalkowski.activitylauncher.domain.usecase.favorites

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import javax.inject.Inject

class GetIsFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) {
    operator fun invoke(componentName: ComponentName): Boolean {
        return favoritesRepository.isFavorite(componentName)
    }
}
