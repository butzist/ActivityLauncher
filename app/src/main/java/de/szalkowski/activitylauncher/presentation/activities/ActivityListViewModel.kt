package de.szalkowski.activitylauncher.presentation.activities

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PackageActivities
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
class ActivityListViewModel @Inject constructor(
    private val packageRepository: PackageRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val packageName: String = savedStateHandle.get<String>("packageName") ?: ""

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var allPackageActivities: PackageActivities? = null
    private var combinedActivities: List<MyActivityInfo> = emptyList()
    private var currentQuery: String = ""
    private var filterJob: Job? = null

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _isSearching.value = true
            val result = withContext(Dispatchers.Default) {
                packageRepository.getActivities(packageName)
            }
            allPackageActivities = result
            combinedActivities = result.activities
            filter(currentQuery)
        }
    }

    fun filter(query: String) {
        currentQuery = query
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                val filtered = withContext(Dispatchers.Default) {
                    performFilter(query)
                }
                _activities.value = filtered
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun performFilter(query: String): List<MyActivityInfo> {
        val pack = allPackageActivities ?: return emptyList()
        if (query.isEmpty()) return combinedActivities

        val packageMatches = pack.packageName.contains(query, ignoreCase = true) ||
            pack.name.contains(query, ignoreCase = true)

        val result = mutableListOf<MyActivityInfo>()

        // Check default activity
        pack.defaultActivity?.let { a ->
            yield()
            if (packageMatches ||
                a.name.contains(query, ignoreCase = true) ||
                a.componentName.className.contains(query, ignoreCase = true)
            ) {
                result.add(a)
            }
        }

        // Check regular activities
        for (a in pack.activities) {
            yield()
            if (packageMatches ||
                a.name.contains(query, ignoreCase = true) ||
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
