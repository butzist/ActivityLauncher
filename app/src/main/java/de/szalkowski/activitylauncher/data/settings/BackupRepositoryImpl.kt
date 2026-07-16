package de.szalkowski.activitylauncher.data.settings

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.BuildConfig
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.FavoriteEntity
import de.szalkowski.activitylauncher.data.database.RecentEntity
import de.szalkowski.activitylauncher.data.database.ShortcutEntity
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
) : BackupRepository {

    override suspend fun exportBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", BuildConfig.VERSION_CODE)

            // Settings
            val settings = JSONObject()
            settings.put("theme", settingsRepository.theme)
            settings.put("language", settingsRepository.language)
            settings.put("hide_private", settingsRepository.hidePrivate)

            // Signature Key
            val signerPrefs = context.getSharedPreferences("signer", Context.MODE_PRIVATE)
            val signatureKey = signerPrefs.getString("key", null)
            if (signatureKey != null) {
                settings.put("signature_key", signatureKey)
            }

            root.put("settings", settings)

            // Favorites
            val favorites = database.favoriteDao().getAll()
            val favoritesArray = JSONArray()
            favorites.forEach { favorite ->
                favoritesArray.put(favoriteToJson(favorite))
            }
            root.put("favorites", favoritesArray)

            // Recents
            val recents = database.recentDao().getAll()
            val recentsArray = JSONArray()
            recents.forEach { recent ->
                recentsArray.put(recentToJson(recent))
            }
            root.put("recents", recentsArray)

            // Shortcuts
            val managedShortcuts = database.shortcutDao().getAll()
            val shortcutsArray = JSONArray()
            managedShortcuts.forEach { shortcut ->
                shortcutsArray.put(shortcutToJson(shortcut))
            }
            root.put("shortcuts", shortcutsArray)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(root.toString(2))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            val root = JSONObject(content)

            // Import Settings
            if (root.has("settings")) {
                val settings = root.getJSONObject("settings")
                val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit {
                    if (settings.has("theme")) putString("theme", settings.getString("theme"))
                    if (settings.has("language")) putString("language", settings.getString("language"))
                    // Note: PREF_HIDE_HIDE_PRIVATE is "hide_hide_private" in SettingsRepositoryImpl
                    if (settings.has("hide_private")) putBoolean("hide_hide_private", settings.getBoolean("hide_private"))
                }

                // Import Signature Key
                if (settings.has("signature_key")) {
                    val signerPrefs = context.getSharedPreferences("signer", Context.MODE_PRIVATE)
                    signerPrefs.edit {
                        putString("key", settings.getString("signature_key"))
                    }
                }
            }

            // Import Favorites
            if (root.has("favorites")) {
                val favoritesArray = root.getJSONArray("favorites")
                for (i in 0 until favoritesArray.length()) {
                    val favorite = jsonToFavorite(favoritesArray.getJSONObject(i))
                    database.favoriteDao().insert(favorite)
                }
            }

            // Import Recents
            if (root.has("recents")) {
                val recentsArray = root.getJSONArray("recents")
                for (i in 0 until recentsArray.length()) {
                    val recent = jsonToRecent(recentsArray.getJSONObject(i))
                    database.recentDao().insert(recent)
                }
            }

            // Import Shortcuts
            if (root.has("shortcuts")) {
                val shortcutsArray = root.getJSONArray("shortcuts")
                for (i in 0 until shortcutsArray.length()) {
                    val shortcut = jsonToShortcut(shortcutsArray.getJSONObject(i))
                    database.shortcutDao().insert(shortcut)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun favoriteToJson(favorite: FavoriteEntity): JSONObject {
        return JSONObject().apply {
            put("packageName", favorite.packageName)
            put("className", favorite.className)
            put("name", favorite.name)
            put("intentUri", favorite.intentUri)
            put("iconBundle", Base64.encodeToString(favorite.iconBundle, Base64.NO_WRAP))
            put("launcherPlugin", favorite.launcherPlugin)
            put("timestamp", favorite.timestamp)
        }
    }

    private fun jsonToFavorite(json: JSONObject): FavoriteEntity {
        return FavoriteEntity(
            packageName = json.getString("packageName"),
            className = json.getString("className"),
            name = json.getString("name"),
            intentUri = json.getString("intentUri"),
            iconBundle = Base64.decode(json.getString("iconBundle"), Base64.NO_WRAP),
            launcherPlugin = if (json.isNull("launcherPlugin")) null else json.getString("launcherPlugin"),
            timestamp = json.getLong("timestamp"),
        )
    }

    private fun recentToJson(recent: RecentEntity): JSONObject {
        return JSONObject().apply {
            put("packageName", recent.packageName)
            put("className", recent.className)
            put("name", recent.name)
            put("intentUri", recent.intentUri)
            put("iconBundle", Base64.encodeToString(recent.iconBundle, Base64.NO_WRAP))
            put("launcherPlugin", recent.launcherPlugin)
            put("timestamp", recent.timestamp)
        }
    }

    private fun jsonToRecent(json: JSONObject): RecentEntity {
        return RecentEntity(
            packageName = json.getString("packageName"),
            className = json.getString("className"),
            name = json.getString("name"),
            intentUri = json.getString("intentUri"),
            iconBundle = Base64.decode(json.getString("iconBundle"), Base64.NO_WRAP),
            launcherPlugin = if (json.isNull("launcherPlugin")) null else json.getString("launcherPlugin"),
            timestamp = json.getLong("timestamp"),
        )
    }

    private fun shortcutToJson(shortcut: ShortcutEntity): JSONObject {
        return JSONObject().apply {
            put("packageName", shortcut.packageName)
            put("className", shortcut.className)
            put("name", shortcut.name)
            put("intentUri", shortcut.intentUri)
            put("iconBundle", Base64.encodeToString(shortcut.iconBundle, Base64.NO_WRAP))
            put("launcherPlugin", shortcut.launcherPlugin)
            put("shortcutPlugin", shortcut.shortcutPlugin)
            put("timestamp", shortcut.timestamp)
        }
    }

    private fun jsonToShortcut(json: JSONObject): ShortcutEntity {
        return ShortcutEntity(
            packageName = json.getString("packageName"),
            className = json.getString("className"),
            name = json.getString("name"),
            intentUri = json.getString("intentUri"),
            iconBundle = Base64.decode(json.getString("iconBundle"), Base64.NO_WRAP),
            launcherPlugin = if (json.isNull("launcherPlugin")) null else json.getString("launcherPlugin"),
            shortcutPlugin = if (json.isNull("shortcutPlugin")) null else json.getString("shortcutPlugin"),
            timestamp = json.getLong("timestamp"),
        )
    }
}
