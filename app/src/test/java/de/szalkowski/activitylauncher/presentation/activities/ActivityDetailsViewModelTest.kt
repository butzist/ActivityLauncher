package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.SavedStateHandle
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.ToggleFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailsViewModelTest {
    private val packageRepository: PackageRepository = mock()
    private val favoritesRepository: FavoritesRepository = mock()
    private val launchActivityUseCase: LaunchActivityUseCase = mock()
    private val createShortcutUseCase: CreateShortcutUseCase = mock()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()
    private val shareActivityUseCase: ShareActivityUseCase = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val iconLoader: IconLoader = mock()
    private val recentsRepository: RecentsRepository = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val componentName = createMockComponentName("com.test", "Activity")
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun createMockComponentName(pkg: String, cls: String): ComponentName = mock {
        on { packageName } doReturn pkg
        on { className } doReturn cls
    }

    private val activityInfo = MyActivityInfo(
        componentName,
        "Test Activity",
        "res:icon",
        false,
    )

    private lateinit var viewModel: ActivityDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        whenever(packageRepository.getActivity(any())).thenReturn(activityInfo)
        whenever(favoritesRepository.isFavorite(any())).thenReturn(false)
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(mock<IconCompat>())
        whenever(launchActivityUseCase.getPlugins()).thenReturn(emptyList())
        whenever(createShortcutUseCase.getPlugins()).thenReturn(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        viewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load activity details on init`() {
        assertNotNull(viewModel.activityInfo.value)
        assertEquals("Test Activity", viewModel.editedName.value)
        assertFalse(viewModel.isFavorite.value)
    }

    @Test
    fun `should toggle favorite`() {
        whenever(favoritesRepository.isFavorite(any())).thenReturn(true)

        viewModel.toggleFavorite()

        verify(toggleFavoriteUseCase).invoke(any())
        assertTrue(viewModel.isFavorite.value)
    }

    @Test
    fun `should launch activity`() {
        viewModel.launchActivity()
        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture(), isNull())
        assertEquals("com.test", captor.firstValue.component.packageName)
        assertEquals("Activity", captor.firstValue.component.className)
    }

    @Test
    fun `should create shortcut`() {
        viewModel.createShortcut()
        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), isNull())
        assertEquals("com.test", captor.firstValue.component.packageName)
        assertEquals("Activity", captor.firstValue.component.className)
    }

    @Test
    fun `should show chooser buttons if multiple handlers exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))

        // Re-init viewModel to pick up new mock values
        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertTrue(newViewModel.showLaunchChooser.value)
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should hide chooser buttons if only one handler exists`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock()))

        // Re-init viewModel to pick up new mock values
        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertFalse(newViewModel.showLaunchChooser.value)
        assertFalse(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should load plugins on init`() {
        val launchPlugin = PluginInfo("Launch Plugin", createMockComponentName("pkg", "cls"), null)
        val shortcutPlugin = PluginInfo("Shortcut Plugin", createMockComponentName("pkg2", "cls2"), null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(shortcutPlugin))

        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertEquals(listOf(launchPlugin), newViewModel.launchPlugins.value)
        assertEquals(listOf(shortcutPlugin), newViewModel.shortcutPlugins.value)
    }

    @Test
    fun `should use selected launch plugin when launching`() {
        val pluginComp = createMockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", pluginComp, null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))
        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)

        newViewModel.launchActivity()

        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture(), eq(pluginComp))
        assertEquals("com.test", captor.firstValue.component.packageName)
        assertEquals("Activity", captor.firstValue.component.className)
    }

    @Test
    fun `should use selected shortcut plugin when creating shortcut`() {
        val pluginComp = createMockComponentName("pkg2", "cls2")
        val shortcutPlugin = PluginInfo("Shortcut Plugin", pluginComp, null)
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(shortcutPlugin))
        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectShortcutPlugin(pluginComp)

        newViewModel.createShortcut()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), eq(pluginComp))
        assertEquals("com.test", captor.firstValue.component.packageName)
        assertEquals("Activity", captor.firstValue.component.className)
    }

    @Test
    fun `should pass launch plugin extra when creating shortcut`() {
        val pluginComp = createMockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", pluginComp, null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))
        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)

        newViewModel.createShortcut()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), isNull())
        assertEquals("com.test", captor.firstValue.component.packageName)
        assertEquals("Activity", captor.firstValue.component.className)
        assertEquals(pluginComp, captor.firstValue.launcherPlugin)
    }

    @Test
    fun `should show launch chooser dots only if multiple launch plugins exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock()))

        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertTrue(newViewModel.showLaunchChooser.value)
        // If multiple launch plugins exist, we also show the shortcut chooser to allow picking the launch plugin for the shortcut
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should show shortcut chooser dots only if multiple shortcut plugins exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))

        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertFalse(newViewModel.showLaunchChooser.value)
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should update edited fields`() {
        viewModel.updateName("New Name")
        assertEquals("New Name", viewModel.editedName.value)

        viewModel.updatePackage("com.new.package")
        assertEquals("com.new.package", viewModel.editedPackage.value)

        viewModel.updateClass("com.new.package.NewActivity")
        assertEquals("com.new.package.NewActivity", viewModel.editedClass.value)
    }

    @Test
    fun `should update icon resource name and load icon`() = runTest {
        val iconRes = "com.test:drawable/icon"
        val mockIcon: IconCompat = mock()
        whenever(iconLoader.tryGetIcon(iconRes)).thenReturn(Result.success(mockIcon))

        viewModel.updateIconResourceName(iconRes)
        testDispatcher.scheduler.runCurrent()

        assertEquals(iconRes, viewModel.editedIconResourceName.value)
        assertEquals(mockIcon, viewModel.editedIcon.value)
    }

    @Test
    fun `should emit error message with debounce when icon loading fails`() = runTest {
        val iconRes = "invalid_icon"
        whenever(iconLoader.tryGetIcon(iconRes)).thenReturn(Result.failure(IconLoader.NullResourceException()))
        val mockIcon: IconCompat = mock()
        whenever(getActivityIconUseCase.invoke(null, componentName)).thenReturn(mockIcon)

        val errorMessages = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.errorMessage.collect { errorMessages.add(it) }
        }

        viewModel.updateIconResourceName(iconRes)

        // Immediately after update, error should NOT be there yet
        assertEquals(0, errorMessages.size)
        assertEquals(mockIcon, viewModel.editedIcon.value)

        // Advance time by 2 seconds
        advanceTimeBy(2000)
        runCurrent()

        assertEquals(1, errorMessages.size)
        assertEquals(R.string.error_invalid_icon_resource, errorMessages[0])

        job.cancel()
    }
}
