package de.szalkowski.activitylauncher.data.packages

import android.content.Context
import android.content.pm.PackageManager
import de.szalkowski.activitylauncher.data.database.AppPackageEntity
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class PackageRepositoryImplTest {
    private val context: Context = mock()
    private val dataSource: PackageDataSource = mock()
    private val packageManager: PackageManager = mock()
    private val allPackagesFlow = MutableStateFlow<List<PackageWithActivities>>(emptyList())
    private lateinit var repository: PackageRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(dataSource.allPackagesFlow).thenReturn(allPackagesFlow)
        runTest {
            whenever(dataSource.removePackage(any())).thenReturn(0)
            whenever(dataSource.clear()).thenReturn(0)
        }

        repository = PackageRepositoryImpl(context, dataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `packagesFlow should be stably sorted by name then package name`() = runTest {
        val p1 = createPackageWithActivities("App", "com.zzz")
        val p2 = createPackageWithActivities("App", "com.aaa")
        val p3 = createPackageWithActivities("Bpp", "com.bbb")

        allPackagesFlow.value = listOf(p1, p3, p2)
        advanceUntilIdle()

        val result = repository.packagesFlow.value

        assertEquals(3, result.size)
        assertEquals("com.aaa", result[0].packageName)
        assertEquals("com.zzz", result[1].packageName)
        assertEquals("com.bbb", result[2].packageName)
    }

    @Test
    fun `loadDetails should call dataSource and finish syncing`() = runTest {
        val packageName = "com.test.app"
        repository.loadDetails(packageName)
        advanceUntilIdle()

        verify(dataSource).loadDetails(packageName)
        assertEquals(false, repository.isSyncing.value)
    }

    @Test
    fun `removePackage should call dataSource and finish syncing`() = runTest {
        val packageName = "com.test.app"
        repository.removePackage(packageName)
        advanceUntilIdle()

        verify(dataSource).removePackage(packageName)
        assertEquals(false, repository.isSyncing.value)
    }

    private fun createPackageWithActivities(name: String, packageName: String) = PackageWithActivities(
        pkg = AppPackageEntity(packageName, name, "1.0", null, true, 0L),
        activities = emptyList(),
    )
}
