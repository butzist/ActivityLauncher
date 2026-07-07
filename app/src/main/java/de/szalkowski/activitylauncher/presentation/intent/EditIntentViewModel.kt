package de.szalkowski.activitylauncher.presentation.intent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EditIntentViewModel @Inject constructor() : ViewModel() {
    private val _intentDef = MutableStateFlow(IntentDef())
    val intentDef: StateFlow<IntentDef> = _intentDef.asStateFlow()

    val isIntentValid: StateFlow<Boolean> = _intentDef.map { def ->
        val categoriesValid = def.categories.all { it.isNotBlank() } &&
            def.categories.distinct().size == def.categories.size

        val extrasValid = def.extras.all { extra ->
            extra.key.isNotBlank() && extra.type.isValid(extra.value)
        } && def.extras.map { it.key }.distinct().size == def.extras.size

        categoriesValid && extrasValid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun init(initialIntentDef: IntentDef) {
        _intentDef.value = initialIntentDef
    }

    fun updateAction(action: String) {
        _intentDef.value = _intentDef.value.copy(action = action)
    }

    fun updateData(data: String) {
        _intentDef.value = _intentDef.value.copy(data = data)
    }

    fun updateMimeType(mimeType: String) {
        _intentDef.value = _intentDef.value.copy(mimeType = mimeType)
    }

    fun addCategory() {
        val current = _intentDef.value
        _intentDef.value = current.copy(categories = current.categories + "")
    }

    fun updateCategory(index: Int, category: String) {
        val current = _intentDef.value
        val updated = current.categories.toMutableList()
        if (index in updated.indices) {
            updated[index] = category
            _intentDef.value = current.copy(categories = updated)
        }
    }

    fun removeCategory(index: Int) {
        val current = _intentDef.value
        val updated = current.categories.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _intentDef.value = current.copy(categories = updated)
        }
    }

    fun addExtra(type: ExtraType = ExtraType.STRING) {
        val current = _intentDef.value
        _intentDef.value = current.copy(extras = current.extras + ExtraDef("", "", type))
    }

    fun updateExtra(index: Int, extra: ExtraDef) {
        val current = _intentDef.value
        val updated = current.extras.toMutableList()
        if (index in updated.indices) {
            updated[index] = extra
            _intentDef.value = current.copy(extras = updated)
        }
    }

    fun removeExtra(index: Int) {
        val current = _intentDef.value
        val updated = current.extras.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _intentDef.value = current.copy(extras = updated)
        }
    }
}
