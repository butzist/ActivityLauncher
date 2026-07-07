package de.szalkowski.activitylauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.favorites.FavoritesRepositoryImpl
import de.szalkowski.activitylauncher.data.recents.RecentsRepositoryImpl
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class StorageMigrationTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Inject
    lateinit var database: AppDatabase

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val iconLoader: IconLoader = mock()

    @BindValue
    val activitySharer: ActivitySharer = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    private val favoritesPrefs = context.getSharedPreferences("al_favorites", Context.MODE_PRIVATE)
    private val recentsPrefs = context.getSharedPreferences("al_recent_activities", Context.MODE_PRIVATE)

    @Before
    fun setup() {
        hiltRule.inject()
        favoritesPrefs.edit().clear().commit()
        recentsPrefs.edit().clear().commit()
        runBlocking {
            database.clearAllTables()
        }

        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(icon)

        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val component = invocation.getArgument<ComponentName>(0)
            MyActivityInfo(component, "Test Activity", iconResourceName = null, isPrivate = false)
        }
    }

    private suspend fun <T> Flow<List<T>>.waitForNotEmpty(): List<T> {
        return withTimeout(5.seconds) {
            this@waitForNotEmpty.first { it.isNotEmpty() }
        }
    }

    @Test
    fun testFavoritesMigration() = runBlocking {
        // 1. Setup legacy data
        val component = ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity")
        favoritesPrefs.edit().putStringSet("favorites", setOf(component.flattenToString())).commit()

        // 2. Initialize repository (triggers migration)
        val repo = FavoritesRepositoryImpl(context, database.favoriteDao(), packageRepository, getActivityIconUseCase)

        // 3. Verify data migrated to Room
        val favorites = repo.getFavoritesFlow().waitForNotEmpty()
        assertEquals(1, favorites.size)
        assertEquals(component, favorites[0].intent.component)
        assertTrue(favoritesPrefs.getBoolean("room_migration_done", false))
    }

    @Test
    fun testRecentsMigration() = runBlocking {
        // 1. Setup legacy data
        val component = ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity")
        val timestamp = System.currentTimeMillis()
        recentsPrefs.edit().putStringSet("recents", setOf("${component.flattenToString()};$timestamp")).commit()

        // 2. Initialize repository (triggers migration)
        val repo = RecentsRepositoryImpl(context, database.recentDao(), packageRepository, getActivityIconUseCase)

        // 3. Verify data migrated to Room
        val recents = repo.getRecentsFlow().waitForNotEmpty()
        assertEquals(1, recents.size)
        assertEquals(component, recents[0].intent.component)
        assertTrue(recentsPrefs.getBoolean("room_migration_done", false))
    }

    @Test
    fun testNewShortcutStorage() = runBlocking {
        val repo = FavoritesRepositoryImpl(context, database.favoriteDao(), packageRepository, getActivityIconUseCase)
        val component = ComponentName("com.test", "com.test.Activity")
        val icon = getActivityIconUseCase(null, component)
        val intent = Intent("com.test.ACTION").setComponent(component).putExtra("test_extra", "value")
        val request = ShortcutRequest("Custom Name", intent, icon)

        repo.addFavorite(request)

        val favorites = repo.getFavoritesFlow().waitForNotEmpty()
        assertEquals(1, favorites.size)
        assertEquals("Custom Name", favorites[0].name)
        assertEquals("com.test.ACTION", favorites[0].intent.action)
        assertEquals("value", favorites[0].intent.getStringExtra("test_extra"))
    }

    @Test
    fun testRecentsDuplicateAndIconPersistence() = runBlocking {
        val repo = RecentsRepositoryImpl(context, database.recentDao(), packageRepository, getActivityIconUseCase)
        val component = ComponentName("com.test", "com.test.Activity")

        // 1. Add first time (system icon)
        val systemIcon = androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
        val request1 = ShortcutRequest("System Name", Intent().setComponent(component), systemIcon)
        repo.addActivity(request1)

        val recents = repo.getRecentsFlow().waitForNotEmpty()
        assertEquals(1, recents.size)
        assertEquals("System Name", recents[0].name)

        // 2. Add second time with custom icon and name (edited on details page)
        val customBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
        val customIcon = androidx.core.graphics.drawable.IconCompat.createWithBitmap(customBitmap)
        val request2 = ShortcutRequest("Custom Name", Intent().setComponent(component), customIcon)
        repo.addActivity(request2)

        // Give it a moment to process the async insert
        Thread.sleep(1000)

        // 3. Verify only 1 entry exists (no duplicate) and it has the CUSTOM data
        val updatedRecents = repo.getRecentsFlow().first()
        assertEquals(1, updatedRecents.size)
        assertEquals("Custom Name", updatedRecents[0].name)
        assertEquals(androidx.core.graphics.drawable.IconCompat.TYPE_BITMAP, updatedRecents[0].icon.type)
    }
}
