package de.szalkowski.activitylauncher.presentation.shortcuts

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val shortcutsRepository: ShortcutsRepository,
) : BaseActivityListViewModel<ShortcutsRepository.ManagedShortcut>(
    getFlow = { shortcutsRepository.getShortcutsFlow() },
    onRemoveItem = { shortcut -> shortcutsRepository.deleteShortcut(shortcut.id) },
) {
    override fun handleSaveResult(result: DetailsResult) {
        when (result) {
            is DetailsResult.Save -> {
                viewModelScope.launch {
                    val id = result.id
                    if (id != null) {
                        shortcutsRepository.updateShortcut(id, result.request)
                    } else {
                        shortcutsRepository.recordShortcut(result.request)
                    }
                }
            }
        }
    }
}
