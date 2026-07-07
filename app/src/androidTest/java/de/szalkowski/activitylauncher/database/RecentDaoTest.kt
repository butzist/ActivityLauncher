package de.szalkowski.activitylauncher.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.data.database.AppDatabase
import de.szalkowski.activitylauncher.data.database.RecentDao
import de.szalkowski.activitylauncher.data.database.RecentEntity
import kotlinx.coroutines.flow.first
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
class RecentDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    private lateinit var recentDao: RecentDao

    @Before
    fun init() {
        hiltRule.inject()
        recentDao = database.recentDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetRecent() = runBlocking {
        val recent = RecentEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            timestamp = 1000L,
        )
        recentDao.insert(recent)
        val loaded = recentDao.getAllFlow().first()
        assertEquals(1, loaded.size)
        assertEquals(recent.name, loaded[0].name)
    }

    @Test
    fun insertDuplicateReplaces() = runBlocking {
        val recent1 = RecentEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity 1",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            timestamp = 1000L,
        )
        val recent2 = RecentEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity 2",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;F.flag=1;end",
            iconBundle = byteArrayOf(4, 5, 6),
            launcherPlugin = "some.plugin",
            timestamp = 2000L,
        )

        recentDao.insert(recent1)
        recentDao.insert(recent2)

        val loaded = recentDao.getAllFlow().first()
        assertEquals(1, loaded.size)
        assertEquals("Test Activity 2", loaded[0].name)
        assertEquals(2000L, loaded[0].timestamp)
        assertEquals(recent2.intentUri, loaded[0].intentUri)
    }

    @Test
    fun trimRecents() = runBlocking {
        // Insert 5 items
        for (i in 1..5) {
            val recent = RecentEntity(
                packageName = "com.test.app$i",
                className = "MainActivity",
                name = "Test Activity $i",
                intentUri = "intent://#Intent;component=com.test.app$i/MainActivity;end",
                iconBundle = byteArrayOf(i.toByte()),
                launcherPlugin = null,
                timestamp = i * 1000L,
            )
            recentDao.insert(recent)
        }

        assertEquals(5, recentDao.getAllFlow().first().size)

        // Trim to 3
        recentDao.trim(3)

        val loaded = recentDao.getAllFlow().first()
        assertEquals(3, loaded.size)
        // Should keep 3, 4, 5 (latest timestamps)
        assertEquals("Test Activity 5", loaded[0].name)
        assertEquals("Test Activity 4", loaded[1].name)
        assertEquals("Test Activity 3", loaded[2].name)
    }

    @Test
    fun deleteByComponent() = runBlocking {
        val recent = RecentEntity(
            packageName = "com.test.app",
            className = "MainActivity",
            name = "Test Activity",
            intentUri = "intent://#Intent;component=com.test.app/MainActivity;end",
            iconBundle = byteArrayOf(1, 2, 3),
            launcherPlugin = null,
            timestamp = 1000L,
        )
        recentDao.insert(recent)
        assertEquals(1, recentDao.getAllFlow().first().size)

        recentDao.deleteByComponent("com.test.app", "MainActivity")
        assertEquals(0, recentDao.getAllFlow().first().size)
    }
}
