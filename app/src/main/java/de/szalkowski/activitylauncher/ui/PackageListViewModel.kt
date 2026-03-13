package de.szalkowski.activitylauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.MyPackageInfo
import de.szalkowski.activitylauncher.services.PackageListService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class PackageListViewModel @Inject constructor(
    private val packageListService: PackageListService,
) : ViewModel() {

    private val _packages = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    val packages: StateFlow<List<MyPackageInfo>> = _packages.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var allPackages: List<MyPackageInfo> = emptyList()
    private var currentQuery: String = ""

    init {
        loadPackages()
    }

    fun loadPackages() {
        viewModelScope.launch {
            _isSearching.value = true
            val result = withContext(Dispatchers.Default) {
                packageListService.packages
            }
            allPackages = result
            filter(currentQuery)
            _isSearching.value = false
        }
    }

    fun filter(query: String) {
        currentQuery = query
        viewModelScope.launch {
            _isSearching.value = true
            val filtered = withContext(Dispatchers.Default) {
                performFilter(query)
            }
            _packages.value = filtered
            _isSearching.value = false
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
