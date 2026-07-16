package de.szalkowski.activitylauncher.domain.shortcuts

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.flow.Flow

interface ShortcutsRepository {
    data class ManagedShortcut(
        val id: String,
        val request: ShortcutRequest,
        val timestamp: Long,
    )

    fun getShortcutsFlow(): Flow<List<ManagedShortcut>>
    suspend fun getShortcut(id: String): ManagedShortcut?
    suspend fun recordShortcut(request: ShortcutRequest, id: String? = null): String
    suspend fun updateShortcut(id: String, request: ShortcutRequest)
    suspend fun deleteShortcut(id: String)
}
