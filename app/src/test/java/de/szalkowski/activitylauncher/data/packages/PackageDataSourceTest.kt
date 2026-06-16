package de.szalkowski.activitylauncher.data.packages

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import de.szalkowski.activitylauncher.data.database.PackageDao
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
        whenever(context.packageManager).thenReturn(packageManager)
        dataSource = PackageDataSource(context, packageDao, settingsRepository)
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

        dataSource.sync()

        verify(packageDao).insertPackage(argThat { packageName == "com.test.app" })
    }
}
