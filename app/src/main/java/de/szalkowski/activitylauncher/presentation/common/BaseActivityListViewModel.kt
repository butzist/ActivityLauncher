package de.szalkowski.activitylauncher.presentation.common

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import kotlinx.coroutines.CoroutineDispatcher
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
    protected var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val _activities = MutableStateFlow<List<MyActivityInfo>>(emptyList())
    val activities: StateFlow<List<MyActivityInfo>> = _activities.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val items = withContext(defaultDispatcher) {
                loadItems()
            }
            _activities.value = items
        }
    }

    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        defaultDispatcher = dispatcher
    }

    fun removeItem(componentName: ComponentName) {
        onRemoveItem(componentName)
        _activities.value = _activities.value.filter { it.componentName != componentName }
    }
}
