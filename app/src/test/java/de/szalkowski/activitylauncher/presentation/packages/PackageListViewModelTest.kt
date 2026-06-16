package de.szalkowski.activitylauncher.presentation.packages

import android.graphics.drawable.Drawable
import de.szalkowski.activitylauncher.domain.model.ActivityName
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PackageListViewModelTest {
    private val packageRepository: PackageRepository = mock()
    private val packagesFlow = MutableStateFlow<List<MyPackageInfo>>(emptyList())
    private lateinit var viewModel: PackageListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(packageRepository.packagesFlow).thenReturn(packagesFlow)
        viewModel = PackageListViewModel(packageRepository)
        viewModel.setDispatcher(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should filter packages by name`() = runTest {
        val p1 = createPackage("App One", "com.one", activities = listOf(ActivityName("Activity One", "Main", "com.one.Main")))
        val p2 = createPackage("App Two", "com.two", activities = listOf(ActivityName("Activity Two", "Main", "com.two.Main")))
        packagesFlow.value = listOf(p1, p2)
        advanceUntilIdle()

        viewModel.filter("One")
        advanceUntilIdle()

        assertEquals(1, viewModel.packages.value.size)
        assertEquals("com.one", viewModel.packages.value[0].packageName)
    }

    @Test
    fun `should filter packages by activity name`() = runTest {
        val a1 = ActivityName("Main", "Main", "com.test.Main")
        val a2 = ActivityName("Settings", "Settings", "com.test.Settings")
        val p1 = createPackage("Test App", "com.test", activities = listOf(a1, a2))
        packagesFlow.value = listOf(p1)
        advanceUntilIdle()

        viewModel.filter("Settings")
        advanceUntilIdle()

        assertEquals(1, viewModel.packages.value.size)
        assertEquals(1, viewModel.packages.value[0].activityNames.size)
        assertEquals("Settings", viewModel.packages.value[0].activityNames[0].name)
    }

    @Test
    fun `should update when repository changes`() = runTest {
        val p1 = createPackage("App One", "com.one", activities = listOf(ActivityName("Main", "Main", "com.one.Main")))
        packagesFlow.value = listOf(p1)
        advanceUntilIdle()

        assertEquals(1, viewModel.packages.value.size)

        val p2 = createPackage("App Two", "com.two", activities = listOf(ActivityName("Main", "Main", "com.two.Main")))
        packagesFlow.value = listOf(p1, p2)
        advanceUntilIdle()

        assertEquals(2, viewModel.packages.value.size)
    }

    private fun createPackage(name: String, packageName: String, activities: List<ActivityName> = emptyList()) = MyPackageInfo(
        id = packageName.hashCode().toLong(),
        packageName = packageName,
        name = name,
        version = "1.0",
        defaultActivityName = null,
        activityNames = activities,
        icon = mock<Drawable>(),
        iconResourceName = null,
        isFullyLoaded = true,
    )
}
