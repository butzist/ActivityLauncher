package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.IconCreatorService
import de.szalkowski.activitylauncher.services.IconLoaderService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import de.szalkowski.activitylauncher.services.SettingsService
import de.szalkowski.activitylauncher.services.ShareActivityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val activityListService: ActivityListService,
    private val favoritesService: FavoritesService,
    private val activityLauncherService: ActivityLauncherService,
    private val iconCreatorService: IconCreatorService,
    private val shareActivityService: ShareActivityService,
    private val iconLoaderService: IconLoaderService,
    private val recentActivitiesService: RecentActivitiesService,
    val settingsService: SettingsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val componentName: ComponentName = savedStateHandle.get<ComponentName>("activityComponentName")
        ?: throw IllegalArgumentException("activityComponentName is required")

    private val _activityInfo = MutableStateFlow<MyActivityInfo?>(null)
    val activityInfo: StateFlow<MyActivityInfo?> = _activityInfo.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _editedName = MutableStateFlow("")
    val editedName: StateFlow<String> = _editedName.asStateFlow()

    private val _editedPackage = MutableStateFlow("")
    val editedPackage: StateFlow<String> = _editedPackage.asStateFlow()

    private val _editedClass = MutableStateFlow("")
    val editedClass: StateFlow<String> = _editedClass.asStateFlow()

    private val _editedIconResourceName = MutableStateFlow("")
    val editedIconResourceName: StateFlow<String> = _editedIconResourceName.asStateFlow()

    private val _editedIconDrawable = MutableStateFlow<Drawable?>(null)
    val editedIconDrawable: StateFlow<Drawable?> = _editedIconDrawable.asStateFlow()

    init {
        loadActivityDetails()
    }

    private fun loadActivityDetails() {
        viewModelScope.launch {
            val info = activityListService.getActivity(componentName)
            _activityInfo.value = info
            _isFavorite.value = favoritesService.isFavorite(componentName)
            
            _editedName.value = info.name
            _editedPackage.value = info.componentName.packageName
            _editedClass.value = info.componentName.className
            _editedIconResourceName.value = info.iconResourceName ?: ""
            _editedIconDrawable.value = info.icon
        }
    }

    fun toggleFavorite() {
        val currentFavorite = _isFavorite.value
        if (currentFavorite) {
            favoritesService.removeFavorite(componentName)
        } else {
            favoritesService.addFavorite(componentName)
        }
        _isFavorite.value = !currentFavorite
    }

    fun updateName(name: String) {
        _editedName.value = name
    }

    fun updatePackage(packageName: String) {
        _editedPackage.value = packageName
    }

    fun updateClass(className: String) {
        _editedClass.value = className
    }

    fun updateIconResourceName(iconResourceName: String) {
        _editedIconResourceName.value = iconResourceName
        _editedIconDrawable.value = iconLoaderService.getIcon(iconResourceName)
    }

    fun createShortcut(asRoot: Boolean) {
        val info = getEditedActivityInfo()
        if (asRoot) {
            iconCreatorService.createRootLauncherIcon(info)
            recentActivitiesService.addActivity(info.componentName, true)
        } else {
            iconCreatorService.createLauncherIcon(info)
            recentActivitiesService.addActivity(info.componentName, false)
        }
    }

    fun launchActivity(asRoot: Boolean) {
        val info = getEditedActivityInfo()
        activityLauncherService.launchActivity(info.componentName, asRoot = asRoot, showToast = true)
        recentActivitiesService.addActivity(info.componentName, asRoot)
    }

    fun shareActivity() {
        val info = getEditedActivityInfo()
        shareActivityService.shareActivity(info.componentName)
    }

    private fun getEditedActivityInfo(): MyActivityInfo {
        val componentName = ComponentName(_editedPackage.value, _editedClass.value)
        return MyActivityInfo(
            componentName,
            _editedName.value,
            _editedIconDrawable.value!!,
            _editedIconResourceName.value.ifBlank { null },
            false
        )
    }
}
