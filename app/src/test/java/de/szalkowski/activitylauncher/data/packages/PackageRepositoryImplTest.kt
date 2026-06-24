package de.szalkowski.activitylauncher.data.packages

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import de.szalkowski.activitylauncher.data.database.ActivityEntity
import de.szalkowski.activitylauncher.data.database.AppPackageEntity
import de.szalkowski.activitylauncher.data.database.PackageWithActivities
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class PackageRepositoryImplTest {
    private val context: Context = mock()
    private val dataSource: PackageDataSource = mock()
    private val packageManager: PackageManager = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val allPackagesFlow = MutableStateFlow<List<PackageWithActivities>>(emptyList())
    private lateinit var repository: PackageRepositoryImpl
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(dataSource.allPackagesFlow).thenReturn(allPackagesFlow)
        whenever(settingsRepository.getLocaleConfiguration()).thenReturn(mock())
        runTest {
            whenever(dataSource.removePackage(any())).thenReturn(0)
            whenever(dataSource.clear()).thenReturn(0)
        }

        repository = PackageRepositoryImpl(context, dataSource, settingsRepository)
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
        runCurrent()

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
        runCurrent()

        verify(dataSource).loadDetails(packageName)
        assertEquals(false, repository.isSyncing.value)
    }

    @Test
    fun `removePackage should call dataSource and finish syncing`() = runTest {
        val packageName = "com.test.app"
        repository.removePackage(packageName)
        runCurrent()

        verify(dataSource).removePackage(packageName)
        assertEquals(false, repository.isSyncing.value)
    }

    @Test
    fun `getActivities should return package activities from local data`() = runTest {
        val packageName = "com.test.app"
        val p1 = createPackageWithActivities(
            "Test App",
            packageName,
            listOf(
                ActivityEntity(1, packageName, "Main", "MainActivity", "com.test.app.MainActivity", true, false, "icon"),
                ActivityEntity(2, packageName, "Sub", "SubActivity", "com.test.app.SubActivity", false, true, null),
            ),
        )
        allPackagesFlow.value = listOf(p1)
        runCurrent()

        mockConstruction(ComponentName::class.java) { mock, context ->
            val pkg = context.arguments()[0] as String
            val cls = context.arguments()[1] as String
            whenever(mock.packageName).thenReturn(pkg)
            whenever(mock.className).thenReturn(cls)
        }.use {
            val result = repository.getActivities(packageName)

            assertEquals(packageName, result.packageName)
            assertEquals("Test App", result.name)
            assertNotNull(result.defaultActivity)
            assertEquals("com.test.app.MainActivity", result.defaultActivity?.componentName?.className)
            assertEquals(2, result.activities.size)
            assertEquals(true, result.activities.find { it.name == "Sub" }?.isPrivate)
        }
    }

    @Test
    fun `getActivity should return activity info from local data`() = runTest {
        val packageName = "com.test.app"
        val className = "com.test.app.MainActivity"

        val component = mock<ComponentName>()
        whenever(component.packageName).thenReturn(packageName)
        whenever(component.className).thenReturn(className)

        val p1 = createPackageWithActivities(
            "Test App",
            packageName,
            listOf(
                ActivityEntity(1, packageName, "Main", "MainActivity", className, true, false, "icon"),
            ),
        )
        allPackagesFlow.value = listOf(p1)
        runCurrent()

        val result = repository.getActivity(component)

        assertEquals(className, result.componentName.className)
        assertEquals("Main", result.name)
        assertEquals("icon", result.iconResourceName)
        assertEquals(false, result.isPrivate)
    }

    @Test
    fun `getActivity should fallback to class name if not found in local data`() = runTest {
        val packageName = "com.unknown.app"
        val className = "com.unknown.app.NewActivity"

        val component = mock<ComponentName>()
        whenever(component.packageName).thenReturn(packageName)
        whenever(component.className).thenReturn(className)

        allPackagesFlow.value = emptyList()
        runCurrent()

        val result = repository.getActivity(component)

        assertEquals(className, result.componentName.className)
        assertEquals("NewActivity", result.name)
    }

    private fun createPackageWithActivities(name: String, packageName: String, activities: List<ActivityEntity> = emptyList()) = PackageWithActivities(
        pkg = AppPackageEntity(packageName, name, "1.0", null, true, 0L),
        activities = activities.map { it.copy(packageName = packageName) },
    )
}
