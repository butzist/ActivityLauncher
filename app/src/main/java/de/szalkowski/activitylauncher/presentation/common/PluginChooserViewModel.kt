package de.szalkowski.activitylauncher.presentation.common

import android.content.ComponentName
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class PluginChooserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _launchPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val launchPlugins: StateFlow<List<PluginInfo>> = _launchPlugins.asStateFlow()

    private val _shortcutPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val shortcutPlugins: StateFlow<List<PluginInfo>> = _shortcutPlugins.asStateFlow()

    private val _selectedLaunchPlugin = MutableStateFlow<PluginInfo?>(null)
    val selectedLaunchPlugin: StateFlow<PluginInfo?> = _selectedLaunchPlugin.asStateFlow()

    private val _selectedShortcutPlugin = MutableStateFlow<PluginInfo?>(null)
    val selectedShortcutPlugin: StateFlow<PluginInfo?> = _selectedShortcutPlugin.asStateFlow()

    val isSelectionComplete = combine(
        _launchPlugins,
        _shortcutPlugins,
        _selectedLaunchPlugin,
        _selectedShortcutPlugin,
    ) { launch, shortcut, selLaunch, selShortcut ->
        val launchRequired = launch.size > 1
        val shortcutRequired = shortcut.size > 1

        val launchDone = !launchRequired || selLaunch != null
        val shortcutDone = !shortcutRequired || selShortcut != null

        launchDone && shortcutDone
    }

    fun setPlugins(launch: List<PluginInfo>, shortcut: List<PluginInfo>) {
        _launchPlugins.value = launch
        _shortcutPlugins.value = shortcut
    }

    fun selectLaunchPlugin(plugin: PluginInfo?) {
        _selectedLaunchPlugin.value = plugin
    }

    fun selectShortcutPlugin(plugin: PluginInfo?) {
        _selectedShortcutPlugin.value = plugin
    }

    fun getResult(): PluginChooserResult {
        return PluginChooserResult(
            selectedLaunchPlugin.value?.componentName,
            selectedShortcutPlugin.value?.componentName,
        )
    }
}

data class PluginChooserResult(
    val launchPlugin: ComponentName?,
    val shortcutPlugin: ComponentName?,
)
