package de.szalkowski.activitylauncher.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseActivityListViewModel<T : Any>(
    private val getFlow: () -> Flow<List<T>>,
    private val onRemoveItem: suspend (T) -> Unit,
) : ViewModel() {

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private var dispatcher: CoroutineDispatcher = Dispatchers.Main

    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    init {
        viewModelScope.launch(dispatcher) {
            getFlow().collectLatest {
                _items.value = it
            }
        }
    }

    fun removeItem(item: T) {
        viewModelScope.launch(dispatcher) {
            onRemoveItem(item)
        }
    }

    open fun handleSaveResult(result: DetailsResult) {
    }
}
