package de.szalkowski.activitylauncher.presentation.shortcuts

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    shortcutsRepository: ShortcutsRepository,
) : BaseActivityListViewModel<ShortcutsRepository.ManagedShortcut>(
    getFlow = { shortcutsRepository.getShortcutsFlow() },
    onRemoveItem = { shortcut -> shortcutsRepository.deleteShortcut(shortcut.id) },
)
