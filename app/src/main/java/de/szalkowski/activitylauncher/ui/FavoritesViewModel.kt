package de.szalkowski.activitylauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ComponentName
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesService: FavoritesService,
    private val activityListService: ActivityListService
) : ViewModel() {

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            val favoriteActivities = favoritesService.getFavorites()
            val activityInfos = withContext(Dispatchers.Default) {
                favoriteActivities.mapNotNull { componentName ->
                    runCatching {
                        activityListService.getActivity(componentName)
                    }.getOrNull()
                }
            }
            _activities.value = activityInfos
        }
    }

    fun removeFavorite(componentName: ComponentName) {
        favoritesService.removeFavorite(componentName)
        _activities.value = _activities.value.filter { it.componentName != componentName }
    }
}
