package de.szalkowski.activitylauncher.presentation.recents

import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListViewModel
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val recentsRepository: RecentsRepository,
) : BaseActivityListViewModel(
    getFlow = { recentsRepository.getRecentsFlow() },
    onRemoveItem = { request -> recentsRepository.removeActivity(request) },
)
