package de.szalkowski.activitylauncher.presentation.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

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
            _isSearching.value = true
            try {
                val filtered = withContext(defaultDispatcher) {
                    performFilter(query)
                }
                _packages.value = filtered
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun performFilter(query: String): List<MyPackageInfo> {
        if (query.isEmpty()) return allPackages

        return allPackages.mapNotNull { p ->
            yield()
            val packageMatches = p.name.contains(query, ignoreCase = true) || p.packageName.contains(query, ignoreCase = true)
            val filteredActivities = p.activityNames.filter { it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }
            val defaultActivity = p.defaultActivityName?.takeIf { packageMatches || it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }

            if (filteredActivities.isNotEmpty() || defaultActivity != null) {
                p.copy(
                    activityNames = filteredActivities,
                    defaultActivityName = defaultActivity,
                )
            } else {
                null
            }
        }
    }
}
