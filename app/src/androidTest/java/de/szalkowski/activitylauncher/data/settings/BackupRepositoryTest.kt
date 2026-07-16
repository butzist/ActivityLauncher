package de.szalkowski.activitylauncher.data.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.FavoriteEntity
import de.szalkowski.activitylauncher.data.database.RecentEntity
import de.szalkowski.activitylauncher.data.database.ShortcutEntity
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Inject
    lateinit var database: AppDatabase

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val backupRepositoryMock: BackupRepository = mock()

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreator: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    val intentSigner: de.szalkowski.activitylauncher.domain.launcher.IntentSigner = mock()

    @BindValue
    val viewIntentParser: de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val getPackageIconUseCase: de.szalkowski.activitylauncher.domain.usecase.packages.GetPackageIconUseCase = mock()

    lateinit var backupRepository: BackupRepositoryImpl

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            database.clearAllTables()
        }
        backupRepository = BackupRepositoryImpl(context, database, settingsRepository)

        // Clear shared preferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().clear().commit()
        context.getSharedPreferences("signer", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testExportAndImport() {
        runBlocking {
            // 1. Prepare data
            whenever(settingsRepository.theme).thenReturn("2") // Dark
            whenever(settingsRepository.language).thenReturn("de")
            whenever(settingsRepository.hidePrivate).thenReturn(true)
            whenever(settingsRepository.allowTapLaunch).thenReturn(true)

            val signerPrefs = context.getSharedPreferences("signer", Context.MODE_PRIVATE)
            val testSignatureKey = "test_key_123"
            signerPrefs.edit().putString("key", testSignatureKey).commit()

            val favorite = FavoriteEntity(
                "com.example",
                "com.example.MainActivity",
                "Example",
                "intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10000000;component=com.example/.MainActivity;end",
                byteArrayOf(1, 2, 3),
                null,
                123456789L,
            )
            database.favoriteDao().insert(favorite)

            val recent = RecentEntity(
                "com.recent",
                "com.recent.Activity",
                "Recent",
                "intent:#Intent;component=com.recent/.Activity;end",
                byteArrayOf(4, 5, 6),
                "com.plugin/.Plugin",
                987654321L,
            )
            database.recentDao().insert(recent)

            val shortcut = ShortcutEntity(
                id = "uuid-shortcut-1",
                packageName = "com.shortcut",
                className = "com.shortcut.Activity",
                name = "Managed Shortcut",
                intentUri = "intent:#Intent;component=com.shortcut/.Activity;end",
                iconBundle = byteArrayOf(7, 8, 9),
                launcherPlugin = null,
                shortcutPlugin = "some.plugin",
                timestamp = 135792468L,
            )
            database.shortcutDao().insert(shortcut)

            val backupFile = File(context.cacheDir, "test_backup.json")
            val uri = Uri.fromFile(backupFile)

            // 2. Export
            val exportResult = backupRepository.exportBackup(uri)
            assertTrue("Export should be successful", exportResult.isSuccess)
            assertTrue("Backup file should exist", backupFile.exists())

            // 3. Clear data for import
            database.clearAllTables()
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().clear().commit()
            context.getSharedPreferences("signer", Context.MODE_PRIVATE).edit().clear().commit()

            // 4. Import
            val importResult = backupRepository.importBackup(uri)
            assertTrue("Import should be successful", importResult.isSuccess)

            // 5. Verify restored data
            val restoredFavorites = database.favoriteDao().getAll()
            assertEquals(1, restoredFavorites.size)
            assertEquals(favorite.packageName, restoredFavorites[0].packageName)
            assertEquals(favorite.name, restoredFavorites[0].name)
            assertArrayEquals(favorite.iconBundle, restoredFavorites[0].iconBundle)

            val restoredRecents = database.recentDao().getAll()
            assertEquals(1, restoredRecents.size)
            assertEquals(recent.packageName, restoredRecents[0].packageName)
            assertEquals(recent.launcherPlugin, restoredRecents[0].launcherPlugin)

            val restoredShortcuts = database.shortcutDao().getAll()
            assertEquals(1, restoredShortcuts.size)
            assertEquals(shortcut.name, restoredShortcuts[0].name)
            assertEquals(shortcut.shortcutPlugin, restoredShortcuts[0].shortcutPlugin)

            assertEquals("2", prefs.getString("theme", null))
            assertEquals("de", prefs.getString("language", null))
            assertTrue(prefs.getBoolean("hide_hide_private", false))
            assertTrue(prefs.getBoolean("allow_tap_launch", false))

            val restoredSignerPrefs = context.getSharedPreferences("signer", Context.MODE_PRIVATE)
            assertEquals(testSignatureKey, restoredSignerPrefs.getString("key", null))

            backupFile.delete()
        }
    }

    @Test
    fun testImportInvalidJson() {
        runBlocking {
            val invalidFile = File(context.cacheDir, "invalid.json")
            invalidFile.writeText("{ invalid json")
            val uri = Uri.fromFile(invalidFile)

            val result = backupRepository.importBackup(uri)
            assertTrue("Import should fail with invalid JSON", result.isFailure)

            invalidFile.delete()
        }
    }
}
