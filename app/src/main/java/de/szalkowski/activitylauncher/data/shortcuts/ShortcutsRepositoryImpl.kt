package de.szalkowski.activitylauncher.data.shortcuts

import android.content.ComponentName
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.SerializationUtils
import de.szalkowski.activitylauncher.data.database.ShortcutEntity
import de.szalkowski.activitylauncher.domain.model.LaunchSource
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

    override suspend fun getShortcut(id: String): ShortcutsRepository.ManagedShortcut? {
        return database.shortcutDao().getById(id)?.toManagedShortcut()
    }

    override suspend fun recordShortcut(request: ShortcutRequest, id: String?): String {
        val component = request.intent.component
        val packageName = component?.packageName ?: ""
        val className = component?.className ?: ""
        val intentUri = SerializationUtils.intentToUri(request.intent)

        val existing = database.shortcutDao().findExisting(packageName, className, request.name, intentUri)
        val resolvedId = id ?: existing?.id ?: java.util.UUID.randomUUID().toString()
        database.shortcutDao().insert(request.toEntity(resolvedId))
        return resolvedId
    }

    override suspend fun updateShortcut(id: String, request: ShortcutRequest) {
        database.shortcutDao().insert(request.toEntity(id))
    }

    override suspend fun deleteShortcut(id: String) {
        database.shortcutDao().deleteById(id)
    }

    private fun ShortcutEntity.toManagedShortcut(): ShortcutsRepository.ManagedShortcut {
        val intent = SerializationUtils.uriToIntent(intentUri)
        val icon = SerializationUtils.byteArrayToIcon(iconBundle)!!
        val launcherPlugin = launcherPlugin?.let { ComponentName.unflattenFromString(it) }

        return ShortcutsRepository.ManagedShortcut(
            id = id,
            request = ShortcutRequest(name, intent, icon, launcherPlugin, source = LaunchSource.SAVED),
            timestamp = timestamp,
        )
    }

    private fun ShortcutRequest.toEntity(id: String): ShortcutEntity {
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
