package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface FavoritesService {
    fun getFavorites(): Set<ComponentName>
    fun addFavorite(componentName: ComponentName)
    fun removeFavorite(componentName: ComponentName)
    fun isFavorite(componentName: ComponentName): Boolean
}

@Singleton
class FavoritesServiceImpl @Inject constructor(
    @ApplicationContext context: Context
) : FavoritesService {
    private val prefs: SharedPreferences = context.getSharedPreferences("al_favorites", Context.MODE_PRIVATE)
    private val favoritesKey = "favorites"

    override fun getFavorites(): Set<ComponentName> {
        val strings = prefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()
        return strings.mapNotNull { ComponentName.unflattenFromString(it) }.toSet()
    }

    override fun addFavorite(componentName: ComponentName) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(componentName)
        val newStringSet = favorites.map { it.flattenToString() }.toSet()
        prefs.edit().putStringSet(favoritesKey, newStringSet).apply()
    }

    override fun removeFavorite(componentName: ComponentName) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(componentName)
        val newStringSet = favorites.map { it.flattenToString() }.toSet()
        prefs.edit().putStringSet(favoritesKey, newStringSet).apply()
    }

    override fun isFavorite(componentName: ComponentName): Boolean {
        return getFavorites().contains(componentName)
    }
}
