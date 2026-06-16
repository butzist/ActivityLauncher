package de.szalkowski.activitylauncher.data.packages

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import de.szalkowski.activitylauncher.data.database.AppPackageEntity
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class PackageDataSourceTest {
    private val context: Context = mock()
    private val packageDao: PackageDao = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val packageManager: PackageManager = mock()
    private lateinit var dataSource: PackageDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(context.packageManager).thenReturn(packageManager)

        dataSource = PackageDataSource(context, packageDao, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sync should insert new packages`() = runTest {
        val installedPkg = createMockPackage("com.test.app", "1.0", 100L)
        whenever(packageManager.getInstalledPackages(any<Int>())).thenReturn(listOf(installedPkg))
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(emptyList()))

        dataSource.sync()

        verify(packageDao).insertPackage(argThat { pkg: AppPackageEntity -> pkg.packageName == "com.test.app" && pkg.version == "1.0 (100)" })
    }

    @Test
    fun `sync should remove uninstalled packages`() = runTest {
        val dbPkg = PackageWithActivities(
            pkg = AppPackageEntity("com.old.app", "Old App", "1.0", null, true, 0L),
            activities = emptyList(),
        )
        whenever(packageManager.getInstalledPackages(any<Int>())).thenReturn(emptyList())
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(listOf(dbPkg)))

        dataSource.sync()

        verify(packageDao).deletePackageByName("com.old.app")
    }

    @Test
    fun `sync should update package if version changed`() = runTest {
        val dbPkg = PackageWithActivities(
            pkg = AppPackageEntity("com.test.app", "Test App", "1.0 (100)", null, true, 0L),
            activities = emptyList(),
        )
        val installedPkg = createMockPackage("com.test.app", "1.1", 101L)

        whenever(packageManager.getInstalledPackages(any<Int>())).thenReturn(listOf(installedPkg))
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(listOf(dbPkg)))

        dataSource.sync()

        verify(packageDao).insertPackage(argThat { pkg: AppPackageEntity -> pkg.packageName == "com.test.app" && pkg.version == "1.1 (101)" })
    }

    @Test
    fun `loadDetails should insert activities`() = runTest {
        val packageName = "com.test.app"
        val packageInfo = createMockPackage(packageName, "1.0", 100L)
        val activity1 = mock<ActivityInfo>()
        activity1.name = "$packageName.MainActivity"
        activity1.packageName = packageName
        whenever(activity1.loadLabel(any())).thenReturn("Main")

        val activity2 = mock<ActivityInfo>()
        activity2.name = "$packageName.SettingsActivity"
        activity2.packageName = packageName
        whenever(activity2.loadLabel(any())).thenReturn("Settings")

        packageInfo.activities = arrayOf(activity1, activity2)

        whenever(packageManager.getPackageInfo(eq(packageName), any<Int>())).thenReturn(packageInfo)
        whenever(settingsRepository.hidePrivate).thenReturn(false)

        dataSource.loadDetails(packageName)

        verify(packageDao).updatePackageDetails(
            argThat { pkg -> pkg.packageName == packageName && pkg.isFullyLoaded },
            argThat { activities -> activities.size == 2 && activities.any { it.fullCls == "$packageName.MainActivity" } },
        )
    }

    @Test
    fun `loading package without activities`() = runTest {
        val packageName = "com.empty.app"
        val packageInfo = createMockPackage(packageName, "1.0", 100L)
        packageInfo.activities = null // Zero activities

        whenever(packageManager.getPackageInfo(eq(packageName), any<Int>())).thenReturn(packageInfo)
        whenever(settingsRepository.hidePrivate).thenReturn(false)

        dataSource.loadDetails(packageName)

        verify(packageDao).updatePackageDetails(
            argThat { pkg -> pkg.packageName == packageName && pkg.isFullyLoaded },
            argThat { activities -> activities.isEmpty() },
        )
    }

    @Test
    fun `loadDetails should mark package as loaded even if getPackageInfo fails`() = runTest {
        val packageName = "com.error.app"
        whenever(packageManager.getPackageInfo(eq(packageName), any<Int>())).thenThrow(RuntimeException("Security error"))

        // We need to mock getPackage from DAO because the fallback uses it
        val existingPkg = AppPackageEntity(packageName, "Error App", "1.0", null, false, 0L)
        whenever(packageDao.getPackage(packageName)).thenReturn(existingPkg)
        whenever(packageDao.insertPackage(any())).thenReturn(0L)

        dataSource.loadDetails(packageName)

        // It should still be marked as fully loaded to avoid retries
        verify(packageDao).insertPackage(argThat { pkg -> pkg.packageName == packageName && pkg.isFullyLoaded })
    }

    @Test
    fun `removePackage should call dao`() = runTest {
        val packageName = "com.test.app"
        dataSource.removePackage(packageName)
        verify(packageDao).deletePackageByName(packageName)
    }

    private fun createMockPackage(packageName: String, versionName: String, versionCode: Long): PackageInfo {
        val pkg = PackageInfo()
        pkg.packageName = packageName
        pkg.versionName = versionName
        pkg.versionCode = versionCode.toInt()
        // Note: PackageInfoCompat might fail to read versionCode if Build.VERSION.SDK_INT is not handled,
        // but in unit tests it usually defaults to 0, which leads to using versionCode field.

        val appInfo = mock<ApplicationInfo>()
        whenever(appInfo.loadLabel(any())).thenReturn(packageName)
        pkg.applicationInfo = appInfo
        return pkg
    }
}
