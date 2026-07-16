package de.szalkowski.activitylauncher.data.shortcuts

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShortcutsRepositoryTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    private lateinit var shortcutsRepository: ShortcutsRepository
    private lateinit var icon: ActivityIcon

    @Before
    fun init() {
        hiltRule.inject()
        shortcutsRepository = ShortcutsRepositoryImpl(database)
        runBlocking {
            database.clearAllTables()
        }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        icon = ActivityIcon.Resource(context.packageName, android.R.mipmap.sym_def_app_icon)
    }

    @Test
    fun testRecordShortcut_PreventsDuplicates() = runBlocking {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val request = ShortcutRequest("Test App", Intent().setComponent(componentName), icon, source = LaunchSource.PRIMARY)

        // Record first time
        shortcutsRepository.recordShortcut(request)
        assertEquals(1, shortcutsRepository.getShortcutsFlow().first().size)

        // Record second time (exact same data)
        shortcutsRepository.recordShortcut(request)

        // Size should still be 1
        val shortcuts = shortcutsRepository.getShortcutsFlow().first()
        assertEquals(1, shortcuts.size)
        assertEquals("Test App", shortcuts[0].request.name)
    }

    @Test
    fun testRecordShortcut_DifferentNames_Allowed() = runBlocking {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val request1 = ShortcutRequest("Name 1", Intent().setComponent(componentName), icon, source = LaunchSource.PRIMARY)
        val request2 = ShortcutRequest("Name 2", Intent().setComponent(componentName), icon, source = LaunchSource.PRIMARY)

        shortcutsRepository.recordShortcut(request1)
        shortcutsRepository.recordShortcut(request2)

        assertEquals(2, shortcutsRepository.getShortcutsFlow().first().size)
    }

    @Test
    fun testRecordShortcut_WithExplicitId() = runBlocking {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val request = ShortcutRequest("Test App", Intent().setComponent(componentName), icon, source = LaunchSource.PRIMARY)
        val explicitId = "explicit-id"

        val id = shortcutsRepository.recordShortcut(request, explicitId)

        assertEquals(explicitId, id)
        val shortcuts = shortcutsRepository.getShortcutsFlow().first()
        assertEquals(1, shortcuts.size)
        assertEquals(explicitId, shortcuts[0].id)
    }
}
