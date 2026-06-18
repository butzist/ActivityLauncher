package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.pm.PackageManager.NameNotFoundException
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.ToggleFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val favoritesRepository: FavoritesRepository,
    private val launchActivityUseCase: LaunchActivityUseCase,
    private val createShortcutUseCase: CreateShortcutUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val shareActivityUseCase: ShareActivityUseCase,
    private val getActivityIconUseCase: GetActivityIconUseCase,
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

    private val _editedIcon = MutableStateFlow<IconCompat?>(null)
    val editedIcon: StateFlow<IconCompat?> = _editedIcon.asStateFlow()

    private val _showLaunchChooser = MutableStateFlow(false)
    val showLaunchChooser: StateFlow<Boolean> = _showLaunchChooser.asStateFlow()

    private val _showShortcutChooser = MutableStateFlow(false)
    val showShortcutChooser: StateFlow<Boolean> = _showShortcutChooser.asStateFlow()

    private val _iconErrorTrigger = MutableStateFlow<String?>(null)

    private val _errorMessage = MutableSharedFlow<Int>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        loadActivityDetails()
        setupIconErrorDebounce()
        _showLaunchChooser.value = launchActivityUseCase.hasMultipleHandlers()
        _showShortcutChooser.value = createShortcutUseCase.hasMultipleHandlers()
    }

    @OptIn(FlowPreview::class)
    private fun setupIconErrorDebounce() {
        viewModelScope.launch {
            _iconErrorTrigger
                .filter { it != null }
                .debounce(2000)
                .collectLatest { iconResourceName ->
                    val result = iconLoader.tryGetIcon(iconResourceName!!)
                    result.onFailure {
                        val errorText = when (it) {
                            is IconLoader.NullResourceException -> R.string.error_invalid_icon_resource
                            is NameNotFoundException -> R.string.error_invalid_icon_resource
                            else -> R.string.error_invalid_icon_format
                        }
                        _errorMessage.emit(errorText)
                    }
                }
        }
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

            _editedIcon.value = getActivityIconUseCase(info.iconResourceName, componentName)
        }
    }

    fun toggleFavorite() {
        toggleFavoriteUseCase(componentName)
        _isFavorite.value = favoritesRepository.isFavorite(componentName)
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
        val result = iconLoader.tryGetIcon(iconResourceName)
        _editedIcon.value = result.getOrElse {
            getActivityIconUseCase(null, componentName)
        }
        _iconErrorTrigger.value = iconResourceName
    }

    fun createShortcut(useChooser: Boolean = false) {
        val info = getEditedActivityInfo()
        createShortcutUseCase(info, useChooser = useChooser)
    }

    fun launchActivity(useChooser: Boolean = false) {
        val info = getEditedActivityInfo()
        launchActivityUseCase(info.componentName, showToast = true, useChooser = useChooser)
    }

    fun shareActivity() {
        val info = getEditedActivityInfo()
        shareActivityUseCase(info.componentName)
    }

    private fun getEditedActivityInfo(): MyActivityInfo {
        val componentName = ComponentName(_editedPackage.value, _editedClass.value)
        return MyActivityInfo(
            componentName,
            _editedName.value,
            _editedIconResourceName.value.ifBlank { null },
            false,
        )
    }
}
