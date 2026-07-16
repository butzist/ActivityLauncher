package de.szalkowski.activitylauncher.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.ShortcutDao
import de.szalkowski.activitylauncher.data.database.ShortcutEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ShortcutDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    private lateinit var shortcutDao: ShortcutDao

    @Before
    fun init() {
        hiltRule.inject()
        shortcutDao = database.shortcutDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetShortcut() = runBlocking {
        val shortcut = ShortcutEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            shortcutPlugin = null,
            timestamp = 1000L,
        )
        val id = shortcutDao.insert(shortcut)
        val loaded = shortcutDao.getAll()
        assertEquals(1, loaded.size)
        assertEquals(shortcut.name, loaded[0].name)
        assertEquals(id, loaded[0].id)
    }

    @Test
    fun deleteById() = runBlocking {
        val shortcut = ShortcutEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            shortcutPlugin = null,
            timestamp = 1000L,
        )
        val id = shortcutDao.insert(shortcut)
        assertEquals(1, shortcutDao.getAll().size)

        shortcutDao.deleteById(id)
        assertEquals(0, shortcutDao.getAll().size)
    }

    @Test
    fun updateShortcut() = runBlocking {
        val shortcut = ShortcutEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Original Name",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            shortcutPlugin = null,
            timestamp = 1000L,
        )
        val id = shortcutDao.insert(shortcut)

        val updatedShortcut = shortcut.copy(id = id, name = "Updated Name")
        shortcutDao.insert(updatedShortcut)

        val loaded = shortcutDao.getAll()
        assertEquals(1, loaded.size)
        assertEquals("Updated Name", loaded[0].name)
    }
}
