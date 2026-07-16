package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntentFromIntentDef
import de.szalkowski.activitylauncher.core.util.getIntentDefFromActivityIntent
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.GetIsFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.ToggleFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailsViewModel @Inject constructor(
    packageRepository: PackageRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    getIsFavoriteUseCase: GetIsFavoriteUseCase,
    private val launchActivityUseCase: LaunchActivityUseCase,
    private val createShortcutUseCase: CreateShortcutUseCase,
    private val shareActivityUseCase: ShareActivityUseCase,
    private val getActivityIconUseCase: GetActivityIconUseCase,
    private val iconLoader: IconLoader,
    val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shortcutRequest: ShortcutRequest = savedStateHandle.get<ShortcutRequest>("shortcutRequest")
        ?: throw IllegalArgumentException("shortcutRequest is required")

    private val shortcutId: String? = savedStateHandle.get<String>("shortcutId")

    private val configuration: DetailsConfiguration = savedStateHandle.get<DetailsConfiguration>("configuration")
        ?: DetailsConfiguration.ALL

    val showCreateShortcut: StateFlow<Boolean> = MutableStateFlow(configuration.showCreateShortcut).asStateFlow()
    val showLaunch: StateFlow<Boolean> = MutableStateFlow(configuration.showLaunch).asStateFlow()
    val showShare: StateFlow<Boolean> = MutableStateFlow(configuration.showShare).asStateFlow()
    val showFavorite: StateFlow<Boolean> = MutableStateFlow(configuration.showFavorite).asStateFlow()
    val showSave: StateFlow<Boolean> = MutableStateFlow(configuration.showSave).asStateFlow()
    val showLaunchPluginSelection: StateFlow<Boolean> = MutableStateFlow(configuration.showLaunchPluginSelection).asStateFlow()

    private val _onResult = MutableSharedFlow<DetailsResult>()
    val onResult = _onResult.asSharedFlow()

    private val componentName: ComponentName? = shortcutRequest.intent.component

    private val _activityInfo = MutableStateFlow<MyActivityInfo?>(null)
    val activityInfo: StateFlow<MyActivityInfo?> = _activityInfo.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _editedName = MutableStateFlow(shortcutRequest.name)
    val editedName: StateFlow<String> = _editedName.asStateFlow()

    private val _editedPackage = MutableStateFlow(shortcutRequest.intent.component?.packageName ?: "")
    val editedPackage: StateFlow<String> = _editedPackage.asStateFlow()

    private val _editedClass = MutableStateFlow(shortcutRequest.intent.component?.className ?: "")
    val editedClass: StateFlow<String> = _editedClass.asStateFlow()

    private val _editedIconResourceName = MutableStateFlow("")
    val editedIconResourceName: StateFlow<String> = _editedIconResourceName.asStateFlow()

    private val _editedIconUri = MutableStateFlow<Uri?>(null)
    val editedIconUri: StateFlow<Uri?> = _editedIconUri.asStateFlow()

    private val _intentDef = MutableStateFlow(getIntentDefFromActivityIntent(shortcutRequest.intent))
    val intentDef: StateFlow<IntentDef> = _intentDef.asStateFlow()

    val canLaunch: StateFlow<Boolean> = combine(_editedPackage, _editedClass) { pkg, cls ->
        pkg.isNotBlank() && cls.isNotBlank()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _editedPackage.value.isNotBlank() && _editedClass.value.isNotBlank(),
    )

    val canCreateShortcut: StateFlow<Boolean> =
        combine(_editedName, _editedPackage, _editedClass) { name, pkg, cls ->
            name.isNotBlank() && pkg.isNotBlank() && cls.isNotBlank()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _editedName.value.isNotBlank() && _editedPackage.value.isNotBlank() && _editedClass.value.isNotBlank(),
        )

    val canShare: StateFlow<Boolean> = combine(_editedPackage, _editedClass) { pkg, cls ->
        pkg.isNotBlank() && cls.isNotBlank()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        _editedPackage.value.isNotBlank() && _editedClass.value.isNotBlank(),
    )

    val canFavorite: StateFlow<Boolean> =
        combine(_editedName, _editedPackage, _editedClass) { name, pkg, cls ->
            name.isNotBlank() && pkg.isNotBlank() && cls.isNotBlank()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _editedName.value.isNotBlank() && _editedPackage.value.isNotBlank() && _editedClass.value.isNotBlank(),
        )

    private val _editedIcon = MutableStateFlow<ActivityIcon?>(shortcutRequest.icon)
    val editedIcon: StateFlow<ActivityIcon?> = _editedIcon.asStateFlow()

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

    private val _isEditMode = MutableStateFlow(shortcutId != null)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        setupIconErrorDebounce()

        val launchPluginList = launchActivityUseCase.getPlugins()
        val shortcutPluginList = createShortcutUseCase.getPlugins()

        _launchPlugins.value = launchPluginList
        _shortcutPlugins.value = shortcutPluginList

        _showLaunchChooser.value = launchPluginList.size > 1
        _showShortcutChooser.value = shortcutPluginList.size > 1 || launchPluginList.size > 1

        componentName?.let {
            val info = runCatching { packageRepository.getActivity(it) }.getOrNull()
            _activityInfo.value = info
            _isFavorite.value = getIsFavoriteUseCase(it)
            _editedIconResourceName.value = info?.iconResourceName ?: ""
        }

        _selectedLaunchPlugin.value = shortcutRequest.launcherPlugin?.let { pluginComp ->
            launchPluginList.find { it.componentName == pluginComp }
        }
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
        val request = getCurrentShortcutRequest()
        _isFavorite.value = toggleFavoriteUseCase(request)
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
        _editedIconUri.value = null
        _editedIconResourceName.value = iconResourceName
        val result = iconLoader.tryGetIcon(iconResourceName)
        _editedIcon.value = result.getOrElse {
            getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
        }
        _iconErrorTrigger.value = iconResourceName
    }

    fun updateIconUri(uri: Uri?) {
        _editedIconResourceName.value = ""
        _editedIconUri.value = uri
        if (uri != null) {
            val result = iconLoader.getIcon(uri)
            _editedIcon.value = result.getOrElse {
                getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
            }
        } else {
            _editedIcon.value = getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
        }
        _iconErrorTrigger.value = null
    }

    fun updateEditedIcon(icon: ActivityIcon?) {
        _editedIconResourceName.value = ""
        _editedIconUri.value = null
        if (icon != null) {
            _editedIcon.value = icon
        } else {
            _editedIcon.value = getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
        }
        _iconErrorTrigger.value = null
    }

    fun updateIntentDef(intentDef: IntentDef) {
        _intentDef.value = intentDef
    }

    fun createShortcut(context: Context? = null) {
        viewModelScope.launch {
            val request = getCurrentShortcutRequest()
            createShortcutUseCase(
                request,
                shortcutId = shortcutId,
                shortcutPlugin = _selectedShortcutPlugin.value?.componentName,
                context = context,
            )
        }
    }

    fun saveShortcut() {
        viewModelScope.launch {
            val request = getCurrentShortcutRequest()
            _onResult.emit(DetailsResult.Save(request, shortcutId))
        }
    }

    fun selectLaunchPlugin(componentName: ComponentName?) {
        _selectedLaunchPlugin.value = _launchPlugins.value.find { it.componentName == componentName }
    }

    fun selectShortcutPlugin(componentName: ComponentName?) {
        _selectedShortcutPlugin.value = _shortcutPlugins.value.find { it.componentName == componentName }
    }

    fun launchActivity(context: Context? = null) {
        val request = getCurrentShortcutRequest()
        launchActivityUseCase(request.toLaunchRequest(LaunchSource.PRIMARY), context)
    }

    fun shareActivity() {
        shareActivityUseCase(ComponentName(_editedPackage.value, _editedClass.value))
    }

    private fun getCurrentShortcutRequest(): ShortcutRequest {
        val packageName = _editedPackage.value
        val className = _editedClass.value
        val component = ComponentName(packageName, className)
        val icon = _editedIcon.value ?: getActivityIconUseCase(_editedIconResourceName.value.ifBlank { null }, component)

        return ShortcutRequest(
            name = _editedName.value,
            intent = getActivityIntentFromIntentDef(component, _intentDef.value),
            icon = icon,
            launcherPlugin = _selectedLaunchPlugin.value?.componentName,
            source = shortcutRequest.source,
        )
    }
}
