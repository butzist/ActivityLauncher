package de.szalkowski.activitylauncher.domain.shortcuts

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.flow.Flow

interface ShortcutsRepository {
    data class ManagedShortcut(
        val id: Long,
        val request: ShortcutRequest,
        val timestamp: Long,
    )

    fun getShortcutsFlow(): Flow<List<ManagedShortcut>>
    suspend fun recordShortcut(request: ShortcutRequest): Long
    suspend fun updateShortcut(id: Long, request: ShortcutRequest)
    suspend fun deleteShortcut(id: Long)
}
