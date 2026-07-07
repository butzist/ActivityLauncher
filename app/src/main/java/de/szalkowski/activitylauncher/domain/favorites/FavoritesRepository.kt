package de.szalkowski.activitylauncher.domain.favorites

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavorites(): Set<ComponentName>
    fun getFavoritesFlow(): Flow<List<ShortcutRequest>>
    fun addFavorite(componentName: ComponentName)
    fun addFavorite(request: ShortcutRequest)
    fun removeFavorite(componentName: ComponentName)
    fun removeFavorite(request: ShortcutRequest)
    fun isFavorite(componentName: ComponentName): Boolean
}
