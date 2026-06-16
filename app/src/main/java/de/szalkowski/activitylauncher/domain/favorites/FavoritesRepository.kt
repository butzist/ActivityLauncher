package de.szalkowski.activitylauncher.domain.favorites

import android.content.ComponentName

interface FavoritesRepository {
    fun getFavorites(): Set<ComponentName>
    fun addFavorite(componentName: ComponentName)
    fun removeFavorite(componentName: ComponentName)
    fun isFavorite(componentName: ComponentName): Boolean
}
