package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val recentActivitiesService: RecentActivitiesService,
    private val activityListService: ActivityListService,
) : ViewModel() {

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    init {
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            val recentActivities = recentActivitiesService.getRecentActivities()
            val activityInfos = withContext(Dispatchers.Default) {
                recentActivities.mapNotNull { recent ->
                    runCatching {
                        activityListService.getActivity(recent.componentName)
                    }.getOrNull()
                }
            }
            _activities.value = activityInfos
        }
    }

    fun removeRecent(componentName: ComponentName) {
        recentActivitiesService.removeActivity(componentName)
        _activities.value = _activities.value.filter { it.componentName != componentName }
    }
}
