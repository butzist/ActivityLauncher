package de.szalkowski.activitylauncher.presentation.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.app.di.DefaultDispatcher
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class PackageListViewModel @Inject constructor(
    private val packageRepository: PackageRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _packages = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    val packages: StateFlow<List<MyPackageInfo>> = _packages.asStateFlow()

    private val _isFiltering = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = combine(_isFiltering, packageRepository.isSyncing) { filtering, syncing ->
        filtering || syncing
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var allPackages: List<MyPackageInfo> = emptyList()
    private var currentQuery: String = ""
    private var filterJob: Job? = null

    init {
        viewModelScope.launch {
            packageRepository.packagesFlow.collect {
                allPackages = it
                filter(currentQuery)
            }
        }
    }

    fun filter(query: String) {
        currentQuery = query
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            try {
                _isFiltering.value = true
                val filtered = withContext(defaultDispatcher) {
                    performFilter(query)
                }
                _packages.value = filtered
            } finally {
                _isFiltering.value = false
            }
        }
    }

    private suspend fun performFilter(query: String): List<MyPackageInfo> {
        return allPackages.mapNotNull { p ->
            yield()
            // If not fully loaded, always show it (it will show with a spinner)
            if (!p.isFullyLoaded) return@mapNotNull p

            val packageMatches = p.name.contains(query, ignoreCase = true) || p.packageName.contains(query, ignoreCase = true)
            val filteredActivities = p.activityNames.filter { it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }
            val defaultActivity = p.defaultActivityName?.takeIf { it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }

            // If package name matches and query is not empty, show all activities in that package
            // Otherwise, show only matching activities
            val finalActivities = if (packageMatches && query.isNotEmpty()) p.activityNames else filteredActivities
            val finalDefaultActivity = if (packageMatches && query.isNotEmpty()) p.defaultActivityName else (defaultActivity ?: p.defaultActivityName?.takeIf { packageMatches })

            val hasActivities = finalActivities.isNotEmpty() || finalDefaultActivity != null

            if (hasActivities) {
                p.copy(
                    activityNames = finalActivities,
                    defaultActivityName = finalDefaultActivity,
                )
            } else {
                // Hide fully loaded packages with no matching activities (or no activities at all)
                null
            }
        }
    }
}
