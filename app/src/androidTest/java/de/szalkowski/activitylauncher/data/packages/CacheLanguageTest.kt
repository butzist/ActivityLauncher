package de.szalkowski.activitylauncher.data.packages

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.FakeSystemPackageRepository
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.model.SystemPackage
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

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Before
    fun init() {
        hiltRule.inject()

        // Setup fake data
        val pkg = SystemPackage("com.test.app", "Test App", "1.0 (1)", null)
        val activities = listOf(
            SystemActivity(ComponentName("com.test.app", "com.test.app.MainActivity"), "Main", null, false),
        )
        systemRepository.addPackage(pkg, activities)
    }

    @Test
    fun testCachePurgeOnInvalidate() = runBlocking {
        // 1. Ensure cache has data
        dataSource.sync()

        val initialPackages = packageDao.getAllPackagesFlow().first()
        assertTrue("Cache should not be empty", initialPackages.isNotEmpty())

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
