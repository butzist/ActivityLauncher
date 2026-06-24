package de.szalkowski.activitylauncher.presentation.common

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseActivityListViewModel(
    private val loadItems: suspend () -> List<SystemActivity>,
    private val onRemoveItem: suspend (ComponentName) -> Unit,
) : ViewModel() {

    private val _activities = MutableStateFlow<List<SystemActivity>>(emptyList())
    val activities: StateFlow<List<SystemActivity>> = _activities.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var dispatcher: CoroutineDispatcher = Dispatchers.Main

    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    fun load() {
        viewModelScope.launch(dispatcher) {
            _isSearching.value = true
            val result = withContext(if (dispatcher == Dispatchers.Main) Dispatchers.Default else dispatcher) {
                loadItems()
            }
            _activities.value = result
            _isSearching.value = false
        }
    }

    fun removeItem(componentName: ComponentName) {
        viewModelScope.launch(dispatcher) {
            onRemoveItem(componentName)
            _activities.value = _activities.value.filter { it.componentName != componentName }
        }
    }
}
