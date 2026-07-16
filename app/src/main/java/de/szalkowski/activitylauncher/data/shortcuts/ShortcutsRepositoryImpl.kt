package de.szalkowski.activitylauncher.data.shortcuts

import android.content.ComponentName
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.SerializationUtils
import de.szalkowski.activitylauncher.data.database.ShortcutEntity
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortcutsRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : ShortcutsRepository {

    override fun getShortcutsFlow(): Flow<List<ShortcutsRepository.ManagedShortcut>> {
        return database.shortcutDao().getAllFlow().map { entities ->
            entities.map { it.toManagedShortcut() }
        }
    }

    override suspend fun recordShortcut(request: ShortcutRequest): Long {
        val component = request.intent.component
        val packageName = component?.packageName ?: ""
        val className = component?.className ?: ""
        val intentUri = SerializationUtils.intentToUri(request.intent)

        val existing = database.shortcutDao().findExisting(packageName, className, request.name, intentUri)
        val id = existing?.id ?: 0L
        return database.shortcutDao().insert(request.toEntity(id))
    }

    override suspend fun updateShortcut(id: Long, request: ShortcutRequest) {
        database.shortcutDao().insert(request.toEntity(id))
    }

    override suspend fun deleteShortcut(id: Long) {
        database.shortcutDao().deleteById(id)
    }

    private fun ShortcutEntity.toManagedShortcut(): ShortcutsRepository.ManagedShortcut {
        val intent = SerializationUtils.uriToIntent(intentUri)
        val icon = SerializationUtils.byteArrayToIcon(iconBundle)!!
        val launcherPlugin = launcherPlugin?.let { ComponentName.unflattenFromString(it) }

        return ShortcutsRepository.ManagedShortcut(
            id = id,
            request = ShortcutRequest(name, intent, icon, launcherPlugin),
            timestamp = timestamp,
        )
    }

    private fun ShortcutRequest.toEntity(id: Long = 0): ShortcutEntity {
        val component = intent.component
        return ShortcutEntity(
            id = id,
            packageName = component?.packageName ?: "",
            className = component?.className ?: "",
            name = name,
            intentUri = SerializationUtils.intentToUri(intent),
            iconBundle = SerializationUtils.iconToByteArray(icon),
            launcherPlugin = launcherPlugin?.flattenToString(),
            shortcutPlugin = null, // Will be updated if needed
            timestamp = System.currentTimeMillis(),
        )
    }
}
