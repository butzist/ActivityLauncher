package de.szalkowski.activitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import de.szalkowski.activitylauncher.database.PackageDao
import de.szalkowski.activitylauncher.services.SettingsService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class PackageRepositoryTest {
    private val context: Context = mock()
    private val packageDao: PackageDao = mock()
    private val settingsService: SettingsService = mock()
    private val packageManager: PackageManager = mock()
    private lateinit var repository: PackageRepository

    @Before
    fun setup() {
        whenever(context.packageManager).thenReturn(packageManager)
        repository = PackageRepository(context, packageDao, settingsService)
    }

    @Test
    fun `sync should insert new packages`() = runTest {
        val installedPkg = mock<PackageInfo>()
        installedPkg.packageName = "com.test.app"
        installedPkg.versionName = "1.0"
        val appInfo = mock<ApplicationInfo>()
        installedPkg.applicationInfo = appInfo

        whenever(packageManager.getInstalledPackages(any<Int>())).thenReturn(listOf(installedPkg))
        whenever(packageDao.getAllPackagesFlow()).thenReturn(flowOf(emptyList()))

        repository.sync()

        verify(packageDao).insertPackage(argThat { packageName == "com.test.app" })
    }
}
