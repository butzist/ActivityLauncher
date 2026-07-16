package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntentFromIntentDef
import de.szalkowski.activitylauncher.core.util.getIntentDefFromActivityIntent
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
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
    private val favoritesRepository: FavoritesRepository,
    private val recentsRepository: RecentsRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val launchActivityUseCase: LaunchActivityUseCase,
    private val createShortcutUseCase: CreateShortcutUseCase,
    private val shareActivityUseCase: ShareActivityUseCase,
    private val getActivityIconUseCase: GetActivityIconUseCase,
    private val iconLoader: IconLoader,
    private val shortcutsRepository: ShortcutsRepository,
    val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shortcutRequest: ShortcutRequest = savedStateHandle.get<ShortcutRequest>("shortcutRequest")
        ?: throw IllegalArgumentException("shortcutRequest is required")

    private val shortcutId: Long = savedStateHandle.get<Long>("shortcutId") ?: 0L

    private val configuration: DetailsConfiguration = savedStateHandle.get<DetailsConfiguration>("configuration")
        ?: DetailsConfiguration.ALL

    val showCreateShortcut: StateFlow<Boolean> = MutableStateFlow(configuration.showCreateShortcut).asStateFlow()
    val showLaunch: StateFlow<Boolean> = MutableStateFlow(configuration.showLaunch).asStateFlow()
    val showShare: StateFlow<Boolean> = MutableStateFlow(configuration.showShare).asStateFlow()
    val showFavorite: StateFlow<Boolean> = MutableStateFlow(configuration.showFavorite).asStateFlow()
    val showSave: StateFlow<Boolean> = MutableStateFlow(configuration.showSave).asStateFlow()
    val showLaunchPluginSelection: StateFlow<Boolean> = MutableStateFlow(configuration.showLaunchPluginSelection).asStateFlow()

    private val _onSaveComplete = MutableSharedFlow<ShortcutRequest>()
    val onSaveComplete = _onSaveComplete.asSharedFlow()

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

    private val _editedIcon = MutableStateFlow<IconCompat?>(shortcutRequest.icon)
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

    private val _isEditMode = MutableStateFlow(shortcutId != 0L)
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
            _isFavorite.value = favoritesRepository.isFavorite(it)
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
        _editedIcon.value = resizeIconIfNeeded(
            result.getOrElse {
                getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
            },
        )
        _iconErrorTrigger.value = iconResourceName
    }

    fun updateIconUri(uri: Uri?) {
        _editedIconResourceName.value = ""
        _editedIconUri.value = uri
        if (uri != null) {
            val result = iconLoader.getIcon(uri)
            _editedIcon.value = resizeIconIfNeeded(
                result.getOrElse {
                    getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value))
                },
            )
        } else {
            _editedIcon.value = resizeIconIfNeeded(getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value)))
        }
        _iconErrorTrigger.value = null
    }

    fun updateEditedIcon(icon: IconCompat?) {
        _editedIconResourceName.value = ""
        _editedIconUri.value = null
        if (icon != null) {
            _editedIcon.value = resizeIconIfNeeded(icon)
        } else {
            _editedIcon.value = resizeIconIfNeeded(getActivityIconUseCase(null, componentName ?: ComponentName(_editedPackage.value, _editedClass.value)))
        }
        _iconErrorTrigger.value = null
    }

    private fun resizeIconIfNeeded(icon: IconCompat): IconCompat {
        // IconCompat.toBundle() includes the bitmap if it's a bitmap-based icon.
        // We ensure it's not too large for Binder/Room.
        val bundle = icon.toBundle()
        val type = bundle.getInt("type")
        if (type == IconCompat.TYPE_BITMAP || type == IconCompat.TYPE_ADAPTIVE_BITMAP) {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable("obj", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable("obj")
            }
            if (bitmap != null) {
                val maxSize = 512 // Increased to accommodate high-res adaptive icons
                if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val newWidth: Int
                    val newHeight: Int
                    if (aspectRatio > 1) {
                        newWidth = maxSize
                        newHeight = (maxSize / aspectRatio).toInt()
                    } else {
                        newHeight = maxSize
                        newWidth = (maxSize * aspectRatio).toInt()
                    }
                    val resized = bitmap.scale(newWidth, newHeight, true)
                    return if (type == IconCompat.TYPE_ADAPTIVE_BITMAP) {
                        IconCompat.createWithAdaptiveBitmap(resized)
                    } else {
                        IconCompat.createWithBitmap(resized)
                    }
                }
            }
        }
        return icon
    }

    fun updateIntentDef(intentDef: IntentDef) {
        _intentDef.value = intentDef
    }

    fun createShortcut() {
        viewModelScope.launch {
            val request = getCurrentShortcutRequest()
            createShortcutUseCase(request, _selectedShortcutPlugin.value?.componentName)
        }
    }

    fun saveShortcut() {
        viewModelScope.launch {
            val request = getCurrentShortcutRequest()
            when (configuration) {
                DetailsConfiguration.FAVORITES -> {
                    favoritesRepository.addFavorite(request)
                }

                DetailsConfiguration.SHORTCUTS -> {
                    if (shortcutId != 0L) {
                        shortcutsRepository.updateShortcut(shortcutId, request)
                    } else {
                        shortcutsRepository.recordShortcut(request)
                    }
                }

                DetailsConfiguration.RECENTS -> {
                    recentsRepository.addActivity(request)
                }

                else -> {
                    // Default to shortcuts if not specified, or do nothing?
                    // "All" tab doesn't have showSave = true, so this is safe.
                    shortcutsRepository.recordShortcut(request)
                }
            }
            _onSaveComplete.emit(request)
        }
    }

    fun saveAsNewShortcut() {
        viewModelScope.launch {
            val request = getCurrentShortcutRequest()
            shortcutsRepository.recordShortcut(request)
        }
    }

    fun selectLaunchPlugin(componentName: ComponentName?) {
        _selectedLaunchPlugin.value = _launchPlugins.value.find { it.componentName == componentName }
    }

    fun selectShortcutPlugin(componentName: ComponentName?) {
        _selectedShortcutPlugin.value = _shortcutPlugins.value.find { it.componentName == componentName }
    }

    fun launchActivity() {
        val request = getCurrentShortcutRequest()
        launchActivityUseCase(
            LaunchRequest(
                intent = request.intent,
                name = request.name,
                icon = request.icon,
                launcherPlugin = request.launcherPlugin,
            ),
        )
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
        )
    }
}
