package de.szalkowski.activitylauncher.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.PackageActivities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class ActivityListViewModel @Inject constructor(
    private val activityListService: ActivityListService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var allPackageActivities: PackageActivities? = null
    private var combinedActivities: List<MyActivityInfo> = emptyList()
    private var currentQuery: String = ""

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _isSearching.value = true
            val result = withContext(Dispatchers.Default) {
                activityListService.getActivities(packageName)
            }
            allPackageActivities = result
            combinedActivities = listOfNotNull(result.defaultActivity) + result.activities
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
            _activities.value = filtered
            _isSearching.value = false
        }
    }

    private suspend fun performFilter(query: String): List<MyActivityInfo> {
        val pack = allPackageActivities ?: return emptyList()
        if (query.isEmpty()) return combinedActivities

        val result = mutableListOf<MyActivityInfo>()

        // Check default activity
        pack.defaultActivity?.let { a ->
            yield()
            if (pack.packageName.contains(query, ignoreCase = true) ||
                pack.name.contains(query, ignoreCase = true) ||
                a.name.contains(query, ignoreCase = true) ||
                a.componentName.className.contains(query, ignoreCase = true)
            ) {
                result.add(a)
            }
        }

        // Check regular activities
        for (a in pack.activities) {
            yield()
            if (a.name.contains(query, ignoreCase = true) ||
                a.componentName.shortClassName.contains(query, ignoreCase = true)
            ) {
                if (!result.contains(a)) {
                    result.add(a)
                }
            }
        }

        return result
    }
}
