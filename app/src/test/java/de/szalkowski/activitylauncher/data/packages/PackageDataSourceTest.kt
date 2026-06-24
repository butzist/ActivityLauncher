package de.szalkowski.activitylauncher.data.packages

import android.content.ComponentName
import de.szalkowski.activitylauncher.data.database.AppPackageEntity
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
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
    private val packageDao: PackageDao = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val systemPackageRepository: SystemPackageRepository = mock()
    private lateinit var dataSource: PackageDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(emptyList()))

        // Mock default behavior for common DAO calls
        runTest {
            whenever(packageDao.getPackage(any())).thenReturn(null)
            whenever(packageDao.insertPackage(any())).thenReturn(0L)
            whenever(packageDao.deleteActivitiesForPackage(any())).thenReturn(0)
            whenever(packageDao.insertActivities(any())).thenReturn(emptyList())
        }

        dataSource = PackageDataSource(packageDao, systemPackageRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sync should insert new packages`() = runTest {
        val installedPkg = SystemPackage("com.test.app", "Test App", "1.0 (100)", null)
        whenever(systemPackageRepository.getInstalledPackages()).thenReturn(listOf(installedPkg))

        dataSource.sync()

        verify(packageDao).insertPackage(argThat { pkg: AppPackageEntity -> pkg.packageName == "com.test.app" && pkg.version == "1.0 (100)" })
    }

    @Test
    fun `sync should remove uninstalled packages`() = runTest {
        val dbPkg = PackageWithActivities(
            pkg = AppPackageEntity("com.old.app", "Old App", "1.0", null, true, 0L),
            activities = emptyList(),
        )
        whenever(systemPackageRepository.getInstalledPackages()).thenReturn(emptyList())
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
        val installedPkg = SystemPackage("com.test.app", "Test App", "1.1 (101)", null)

        whenever(systemPackageRepository.getInstalledPackages()).thenReturn(listOf(installedPkg))
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(listOf(dbPkg)))

        dataSource.sync()

        verify(packageDao).insertPackage(argThat { pkg: AppPackageEntity -> pkg.packageName == "com.test.app" && pkg.version == "1.1 (101)" })
    }

    @Test
    fun `loadDetails should insert activities`() = runTest {
        val packageName = "com.test.app"
        val systemPackage = SystemPackage(packageName, "Test App", "1.0 (100)", "pkg_icon")
        val systemActivities = listOf(
            createSystemActivity(packageName, "$packageName.MainActivity", "Main", "icon1", false),
            createSystemActivity(packageName, "$packageName.SettingsActivity", "Settings", "icon2", false),
        )

        whenever(systemPackageRepository.getPackageDetails(packageName)).thenReturn(systemPackage)
        whenever(systemPackageRepository.getActivities(packageName)).thenReturn(systemActivities)
        whenever(settingsRepository.hidePrivate).thenReturn(false)

        dataSource.loadDetails(packageName)

        // Verify split DAO calls
        verify(packageDao, atLeastOnce()).insertPackage(argThat { pkg -> pkg.packageName == packageName && pkg.isFullyLoaded && pkg.iconResourceName == "pkg_icon" })
        verify(packageDao).deleteActivitiesForPackage(packageName)
        verify(packageDao).insertActivities(argThat { activities -> activities.size == 2 && activities.any { it.fullCls == "$packageName.MainActivity" && it.iconResourceName == "icon1" } })
    }

    @Test
    fun `loadDetails should filter private activities if enabled`() = runTest {
        val packageName = "com.test.app"
        val systemPackage = SystemPackage(packageName, "Test App", "1.0 (100)", null)
        val systemActivities = listOf(
            createSystemActivity(packageName, "$packageName.PublicActivity", "Public", null, false),
            createSystemActivity(packageName, "$packageName.PrivateActivity", "Private", null, true),
        )

        whenever(systemPackageRepository.getPackageDetails(packageName)).thenReturn(systemPackage)
        whenever(systemPackageRepository.getActivities(packageName)).thenReturn(systemActivities)
        whenever(settingsRepository.hidePrivate).thenReturn(true)

        dataSource.loadDetails(packageName)

        verify(packageDao).insertActivities(argThat { activities -> activities.size == 1 && activities[0].fullCls == "$packageName.PublicActivity" })
    }

    private fun createSystemActivity(packageName: String, className: String, name: String, icon: String?, isPrivate: Boolean): SystemActivity {
        val componentName = mock<ComponentName>()
        whenever(componentName.packageName).thenReturn(packageName)
        whenever(componentName.className).thenReturn(className)
        return SystemActivity(componentName, name, icon, isPrivate)
    }

    @Test
    fun `loadDetails should delete package if not found in system`() = runTest {
        val packageName = "com.gone.app"
        whenever(systemPackageRepository.getPackageDetails(packageName)).thenReturn(null)

        dataSource.loadDetails(packageName)

        verify(packageDao).deletePackageByName(packageName)
    }

    @Test
    fun `loadDetails should handle error by marking as loaded`() = runTest {
        val packageName = "com.error.app"
        whenever(systemPackageRepository.getPackageDetails(packageName)).thenThrow(RuntimeException("System error"))

        val dbPkg = AppPackageEntity(packageName, "Error App", "1.0", null, false, 0L)
        whenever(packageDao.getPackage(packageName)).thenReturn(dbPkg)

        dataSource.loadDetails(packageName)

        verify(packageDao, atLeastOnce()).insertPackage(argThat { pkg: AppPackageEntity -> pkg.packageName == packageName && pkg.isFullyLoaded })
    }

    @Test
    fun `removePackage should call dao`() = runTest {
        val packageName = "com.test.app"
        dataSource.removePackage(packageName)
        verify(packageDao).deletePackageByName(packageName)
    }
}
