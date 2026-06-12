package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.szalkowski.activitylauncher.services.MyActivityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class BaseActivityListViewModel(
    private val loadItems: suspend () -> List<MyActivityInfo>,
    private val onRemoveItem: (ComponentName) -> Unit,
) : ViewModel() {

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.Default) {
                loadItems()
            }
            _activities.value = items
        }
    }

    fun removeItem(componentName: ComponentName) {
        onRemoveItem(componentName)
        _activities.value = _activities.value.filter { it.componentName != componentName }
    }
}
