package de.szalkowski.activitylauncher.presentation.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.Dispatchers
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
) : ViewModel() {
    private var defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default

    // Internal for testing
    internal fun setDispatcher(dispatcher: kotlinx.coroutines.CoroutineDispatcher) {
        defaultDispatcher = dispatcher
    }

    private val _packages = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    val packages: StateFlow<List<MyPackageInfo>> = _packages.asStateFlow()

    private val _isFiltering = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = combine(_isFiltering, packageRepository.isSyncing) { filtering, syncing ->
        filtering || syncing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
            _isFiltering.value = true
            try {
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
            val defaultActivity = p.defaultActivityName?.takeIf { packageMatches || it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }

            val hasActivities = filteredActivities.isNotEmpty() || defaultActivity != null

            if (hasActivities) {
                p.copy(
                    activityNames = filteredActivities,
                    defaultActivityName = defaultActivity,
                )
            } else {
                // Hide fully loaded packages with no matching activities (or no activities at all)
                null
            }
        }
    }
}
