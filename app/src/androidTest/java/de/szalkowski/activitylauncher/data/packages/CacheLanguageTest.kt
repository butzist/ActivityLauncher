package de.szalkowski.activitylauncher.data.packages

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.data.database.PackageDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CacheLanguageTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dataSource: PackageDataSource

    @Inject
    lateinit var packageDao: PackageDao

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testCachePurgeOnInvalidate() = runBlocking {
        // 1. Ensure cache has data
        dataSource.sync()

        val initialPackages = packageDao.getAllPackagesFlow().first()
        if (initialPackages.isEmpty()) return@runBlocking

        // 2. Clear cache via data source (simulating invalidate behavior)
        dataSource.clear()

        val afterClearPackages = packageDao.getAllPackagesFlow().first()
        assertTrue("Cache should be empty after clear", afterClearPackages.isEmpty())

        // 3. Sync again
        dataSource.sync()
        val afterSyncPackages = packageDao.getAllPackagesFlow().first()
        assertTrue("Cache should be repopulated after sync", afterSyncPackages.isNotEmpty())
    }
}
