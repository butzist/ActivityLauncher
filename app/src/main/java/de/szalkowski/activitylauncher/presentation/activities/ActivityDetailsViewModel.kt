package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
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
    packageRepository: PackageRepository,
    private val favoritesRepository: FavoritesRepository,
    private val launchActivityUseCase: LaunchActivityUseCase,
    private val createShortcutUseCase: CreateShortcutUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val shareActivityUseCase: ShareActivityUseCase,
    private val getActivityIconUseCase: GetActivityIconUseCase,
    private val iconLoader: IconLoader,
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

    private val _launchPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val launchPlugins: StateFlow<List<PluginInfo>> = _launchPlugins.asStateFlow()

    private val _shortcutPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val shortcutPlugins: StateFlow<List<PluginInfo>> = _shortcutPlugins.asStateFlow()

    private val _selectedLaunchPlugin = MutableStateFlow<PluginInfo?>(null)
    val selectedLaunchPlugin: StateFlow<PluginInfo?> = _selectedLaunchPlugin.asStateFlow()

    private val _selectedShortcutPlugin = MutableStateFlow<PluginInfo?>(null)
    val selectedShortcutPlugin: StateFlow<PluginInfo?> = _selectedShortcutPlugin.asStateFlow()

    private val _iconErrorTrigger = MutableStateFlow<String?>(null)

    private val _errorMessage = MutableSharedFlow<Int>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        setupIconErrorDebounce()

        val launchPluginList = launchActivityUseCase.getPlugins()
        val shortcutPluginList = createShortcutUseCase.getPlugins()

        _launchPlugins.value = launchPluginList
        _shortcutPlugins.value = shortcutPluginList

        _showLaunchChooser.value = launchPluginList.size > 1
        _showShortcutChooser.value = shortcutPluginList.size > 1 || launchPluginList.size > 1

        val info = packageRepository.getActivity(componentName)
        _activityInfo.value = info
        _isFavorite.value = favoritesRepository.isFavorite(componentName)

        _editedName.value = info.name
        _editedPackage.value = info.componentName.packageName
        _editedClass.value = info.componentName.className
        _editedIconResourceName.value = info.iconResourceName ?: ""

        _editedIcon.value = getActivityIconUseCase(info.iconResourceName, componentName)
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

    fun createShortcut() {
        val info = getEditedActivityInfo()
        val icon = _editedIcon.value ?: getActivityIconUseCase(info.iconResourceName, info.componentName)
        val request = ShortcutRequest(
            name = info.name,
            component = info.componentName,
            icon = icon,
            extras = Bundle(),
            launcherPlugin = _selectedLaunchPlugin.value?.componentName,
        )
        createShortcutUseCase(request, _selectedShortcutPlugin.value?.componentName)
    }

    fun selectLaunchPlugin(componentName: ComponentName?) {
        _selectedLaunchPlugin.value = _launchPlugins.value.find { it.componentName == componentName }
    }

    fun selectShortcutPlugin(componentName: ComponentName?) {
        _selectedShortcutPlugin.value = _shortcutPlugins.value.find { it.componentName == componentName }
    }

    fun launchActivity() {
        val info = getEditedActivityInfo()
        val request = LaunchRequest(
            component = info.componentName,
            extras = Bundle(),
        )
        launchActivityUseCase(request, _selectedLaunchPlugin.value?.componentName)
    }

    fun shareActivity() {
        val info = getEditedActivityInfo()
        shareActivityUseCase(info.componentName)
    }

    private fun getEditedActivityInfo(): MyActivityInfo {
        val packageName = _editedPackage.value
        val className = _editedClass.value
        val componentName = if (packageName == this.componentName.packageName && className == this.componentName.className) {
            this.componentName
        } else if (packageName.isNotEmpty() && className.isNotEmpty()) {
            ComponentName(packageName, className)
        } else {
            this.componentName
        }

        return MyActivityInfo(
            componentName,
            _editedName.value,
            _editedIconResourceName.value.ifBlank { null },
            false,
        )
    }
}
