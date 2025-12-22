package de.szalkowski.activitylauncher.services

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("al_favorites", Context.MODE_PRIVATE)
    private val favoritesKey = "favorites"

    fun getFavorites(): Set<ComponentName> {
        val strings = prefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()
        return strings.mapNotNull { ComponentName.unflattenFromString(it) }.toSet()
    }

    fun addFavorite(componentName: ComponentName) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(componentName)
        val newStringSet = favorites.map { it.flattenToString() }.toSet()
        prefs.edit().putStringSet(favoritesKey, newStringSet).apply()
    }

    fun removeFavorite(componentName: ComponentName) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(componentName)
        val newStringSet = favorites.map { it.flattenToString() }.toSet()
        prefs.edit().putStringSet(favoritesKey, newStringSet).apply()
    }

    fun isFavorite(componentName: ComponentName): Boolean {
        return getFavorites().contains(componentName)
    }
}
