package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val favoritesRepository: FavoritesRepository,
    private val activityLauncher: ActivityLauncher,
    private val shortcutCreator: ShortcutCreator,
    private val activitySharer: ActivitySharer,
    private val iconLoader: IconLoader,
    private val recentsRepository: RecentsRepository,
    val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
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
            val info = activityRepository.getActivity(componentName)
            _activityInfo.value = info
            _isFavorite.value = favoritesRepository.isFavorite(componentName)

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
            favoritesRepository.removeFavorite(componentName)
        } else {
            favoritesRepository.addFavorite(componentName)
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
        _editedIconDrawable.value = iconLoader.getIcon(iconResourceName)
    }

    fun createShortcut(asRoot: Boolean) {
        val info = getEditedActivityInfo()
        if (asRoot) {
            shortcutCreator.createRootLauncherIcon(info)
            recentsRepository.addActivity(info.componentName, true)
        } else {
            shortcutCreator.createLauncherIcon(info)
            recentsRepository.addActivity(info.componentName, false)
        }
    }

    fun launchActivity(asRoot: Boolean) {
        val info = getEditedActivityInfo()
        activityLauncher.launchActivity(info.componentName, asRoot = asRoot, showToast = true)
        recentsRepository.addActivity(info.componentName, asRoot)
    }

    fun shareActivity() {
        val info = getEditedActivityInfo()
        activitySharer.shareActivity(info.componentName)
    }

    private fun getEditedActivityInfo(): MyActivityInfo {
        val componentName = ComponentName(_editedPackage.value, _editedClass.value)
        return MyActivityInfo(
            componentName,
            _editedName.value,
            _editedIconDrawable.value!!,
            _editedIconResourceName.value.ifBlank { null },
            false,
        )
    }
}
