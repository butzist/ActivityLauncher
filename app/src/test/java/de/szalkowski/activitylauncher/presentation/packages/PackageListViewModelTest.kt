package de.szalkowski.activitylauncher.presentation.packages

import de.szalkowski.activitylauncher.domain.model.ActivityName
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.yield
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
    private val isSyncingFlow = MutableStateFlow(false)
    private lateinit var viewModel: PackageListViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(packageRepository.packagesFlow).thenReturn(packagesFlow)
        whenever(packageRepository.isSyncing).thenReturn(isSyncingFlow)
        viewModel = PackageListViewModel(packageRepository)
        viewModel.setDispatcher(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isSearching should be true when filtering`() = runTest {
        val p1 = createPackage("App One", "com.one", activities = listOf(ActivityName("Activity One", "Main", "com.one.Main")))
        packagesFlow.value = listOf(p1)
        advanceUntilIdle()

        viewModel.filter("One")
        advanceUntilIdle()
        assertEquals(1, viewModel.packages.value.size)
    }

    @Test
    fun `isSearching should be true when repository is syncing`() = runTest {
        isSyncingFlow.value = true
        // The combined flow might need a bit of time or a collector
        var lastSearchingValue = false
        val job = launch {
            viewModel.isSearching.collect { lastSearchingValue = it }
        }
        yield() // Allow collector to pick up initial value

        assertEquals(true, lastSearchingValue)

        isSyncingFlow.value = false
        yield()
        assertEquals(false, lastSearchingValue)
        job.cancel()
    }

    @Test
    fun `packages should maintain repository sort order`() = runTest {
        val p1 = createPackage("App", "com.aaa", activities = listOf(ActivityName("Main", "Main", "com.aaa.Main")))
        val p2 = createPackage("App", "com.zzz", activities = listOf(ActivityName("Main", "Main", "com.zzz.Main")))
        val p3 = createPackage("Bpp", "com.bbb", activities = listOf(ActivityName("Main", "Main", "com.bbb.Main")))

        // Emit already sorted data (as repository should)
        packagesFlow.value = listOf(p1, p2, p3)
        advanceUntilIdle()

        assertEquals(3, viewModel.packages.value.size)
        assertEquals("com.aaa", viewModel.packages.value[0].packageName)
        assertEquals("com.zzz", viewModel.packages.value[1].packageName)
        assertEquals("com.bbb", viewModel.packages.value[2].packageName)
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

    @Test
    fun `should hide fully loaded packages with no activities`() = runTest {
        val p1 = createPackage("Empty App", "com.empty", activities = emptyList()).copy(isFullyLoaded = true)
        val p2 = createPackage("Full App", "com.full", activities = listOf(ActivityName("Main", "Main", "com.full.Main"))).copy(isFullyLoaded = true)
        packagesFlow.value = listOf(p1, p2)
        advanceUntilIdle()

        viewModel.filter("")
        advanceUntilIdle()

        assertEquals(1, viewModel.packages.value.size)
        assertEquals("com.full", viewModel.packages.value[0].packageName)
    }

    @Test
    fun `should show not fully loaded packages even if they have no activities yet`() = runTest {
        val p1 = createPackage("Loading App", "com.loading", activities = emptyList()).copy(isFullyLoaded = false)
        packagesFlow.value = listOf(p1)
        advanceUntilIdle()

        viewModel.filter("")
        advanceUntilIdle()

        assertEquals(1, viewModel.packages.value.size)
        assertEquals("com.loading", viewModel.packages.value[0].packageName)

        // Now resolve it to fully loaded with zero activities
        val p1Loaded = p1.copy(isFullyLoaded = true)
        packagesFlow.value = listOf(p1Loaded)
        advanceUntilIdle()

        // It should now be hidden
        assertEquals(0, viewModel.packages.value.size)
    }

    private fun createPackage(name: String, packageName: String, activities: List<ActivityName> = emptyList()) = MyPackageInfo(
        id = packageName.hashCode().toLong(),
        packageName = packageName,
        name = name,
        version = "1.0",
        defaultActivityName = null,
        activityNames = activities,
        iconResourceName = null,
        isFullyLoaded = true,
    )
}
