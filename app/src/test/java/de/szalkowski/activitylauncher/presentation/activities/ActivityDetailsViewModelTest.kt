package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.Intent
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.SavedStateHandle
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntentFromIntentDef
import de.szalkowski.activitylauncher.core.util.getIntentDefFromActivityIntent
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
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
import org.mockito.ArgumentMatchers
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailsViewModelTest {
    private val packageRepository: PackageRepository = mock()
    private val favoritesRepository: FavoritesRepository = mock()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()
    private val launchActivityUseCase: LaunchActivityUseCase = mock()
    private val createShortcutUseCase: CreateShortcutUseCase = mock()
    private val shareActivityUseCase: ShareActivityUseCase = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val iconLoader: IconLoader = mock()
    private val shortcutsRepository: ShortcutsRepository = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val recentsRepository: RecentsRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var componentName: ComponentName
    private val activityInfo by lazy {
        MyActivityInfo(
            componentName,
            "Test Activity",
            "res:icon",
            false,
        )
    }

    private val mockIcon: IconCompat = mockIcon()
    private lateinit var shortcutRequest: ShortcutRequest
    private lateinit var mockIntent: Intent
    private lateinit var mockedUtil: MockedStatic<*>
    private lateinit var viewModel: ActivityDetailsViewModel

    private fun mockIntent(name: String): Intent = mock {
        on { toString() } doReturn name
        on { component } doReturn componentName
        on { toUri(ArgumentMatchers.anyInt()) } doReturn "intent:#Intent;component=com.test/Activity;end"
    }

    private fun mockComponentName(pkg: String, cls: String): ComponentName = mock {
        on { packageName } doReturn pkg
        on { className } doReturn cls
        on { toString() } doReturn "$pkg/$cls"
    }

    private fun mockIcon(): IconCompat {
        val icon = mock<IconCompat>()
        val bundle = android.os.Bundle().apply { putInt("type", IconCompat.TYPE_RESOURCE) }
        doReturn(bundle).whenever(icon).toBundle()
        return icon
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        componentName = mockComponentName("com.test", "Activity")

        val utilClass = Class.forName("de.szalkowski.activitylauncher.core.util.ActivityIntentKt")
        mockedUtil = mockStatic(utilClass)

        mockIntent = mockIntent("mockIntent")

        mockedUtil.`when`<Any> {
            getIntentDefFromActivityIntent(any())
        }.thenReturn(de.szalkowski.activitylauncher.domain.intent.IntentDef())

        shortcutRequest = ShortcutRequest("Test Activity", mockIntent, mockIcon)

        val defaultIcon = mockIcon()
        whenever(packageRepository.getActivity(any())).thenReturn(activityInfo)
        whenever(favoritesRepository.isFavorite(any())).thenReturn(false)
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(defaultIcon)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(emptyList())
        whenever(createShortcutUseCase.getPlugins()).thenReturn(emptyList())

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        viewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )
    }

    @After
    fun tearDown() {
        mockedUtil.close()
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
        whenever(toggleFavoriteUseCase(any())).thenReturn(true)

        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(mockIntent)

        viewModel.toggleFavorite()

        verify(toggleFavoriteUseCase).invoke(any())
        assertTrue(viewModel.isFavorite.value)

        whenever(toggleFavoriteUseCase(any())).thenReturn(false)
        viewModel.toggleFavorite()
        assertFalse(viewModel.isFavorite.value)
    }

    @Test
    fun `should launch activity`() {
        val launchIntent = mockIntent("launchIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(launchIntent)

        viewModel.launchActivity()
        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture())
        assertNotNull(captor.firstValue.intent)
    }

    @Test
    fun `should create shortcut`() = runTest {
        val shortcutIntent = mockIntent("shortcutIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(shortcutIntent)

        viewModel.createShortcut()
        runCurrent()
        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), anyOrNull())
        assertNotNull(captor.firstValue.intent)
    }

    @Test
    fun `should show chooser buttons if multiple handlers exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))

        // Re-init viewModel to pick up new mock values
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertTrue(newViewModel.showLaunchChooser.value)
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should hide chooser buttons if only one handler exists`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock()))

        // Re-init viewModel to pick up new mock values
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertFalse(newViewModel.showLaunchChooser.value)
        assertFalse(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should route save to favorites repository when configuration is FAVORITES`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.FAVORITES,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(mockIntent)

        val saveCompleteResults = mutableListOf<ShortcutRequest>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            newViewModel.onSaveComplete.collect { saveCompleteResults.add(it) }
        }

        newViewModel.saveShortcut()
        runCurrent()

        verify(favoritesRepository).addFavorite(any<ShortcutRequest>())
        verify(shortcutsRepository, never()).recordShortcut(any())
        verify(recentsRepository, never()).addActivity(any<ShortcutRequest>())
        assertEquals(1, saveCompleteResults.size)
        job.cancel()
    }

    @Test
    fun `should route save to shortcuts repository when configuration is SHORTCUTS`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.SHORTCUTS,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(mockIntent)

        newViewModel.saveShortcut()
        runCurrent()

        verify(shortcutsRepository).recordShortcut(any())
        verify(favoritesRepository, never()).addFavorite(any<ShortcutRequest>())
        verify(recentsRepository, never()).addActivity(any<ShortcutRequest>())
    }

    @Test
    fun `should route save to recents repository when configuration is RECENTS`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.RECENTS,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(mockIntent)

        newViewModel.saveShortcut()
        runCurrent()

        verify(recentsRepository).addActivity(any<ShortcutRequest>())
        verify(favoritesRepository, never()).addFavorite(any<ShortcutRequest>())
        verify(shortcutsRepository, never()).recordShortcut(any())
    }

    @Test
    fun `should load plugins on init`() {
        val launchPluginComp = mockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", launchPluginComp, null)
        val shortcutPluginComp = mockComponentName("pkg2", "cls2")
        val shortcutPlugin = PluginInfo("Shortcut Plugin", shortcutPluginComp, null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(shortcutPlugin))

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertEquals(listOf(launchPlugin), newViewModel.launchPlugins.value)
        assertEquals(listOf(shortcutPlugin), newViewModel.shortcutPlugins.value)
    }

    @Test
    fun `should use selected launch plugin when launching`() {
        val pluginComp = mockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", pluginComp, null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))

        val launchIntent = mockIntent("launchIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(launchIntent)

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)
        newViewModel.launchActivity()

        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture())
        assertNotNull(captor.firstValue.intent)
        assertEquals(pluginComp, captor.firstValue.launcherPlugin)
    }

    @Test
    fun `should use selected shortcut plugin when creating shortcut`() = runTest {
        val pluginComp = mockComponentName("pkg2", "cls2")
        val shortcutPlugin = PluginInfo("Shortcut Plugin", pluginComp, null)
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(shortcutPlugin))

        val shortcutIntent = mockIntent("shortcutIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(shortcutIntent)

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        newViewModel.selectShortcutPlugin(pluginComp)
        newViewModel.createShortcut()
        runCurrent()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), anyOrNull())
        assertNotNull(captor.firstValue.intent)
    }

    @Test
    fun `should pass launch plugin extra when creating shortcut`() = runTest {
        val pluginComp = mockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", pluginComp, null)
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))

        val shortcutIntent = mockIntent("shortcutIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(shortcutIntent)

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)
        newViewModel.createShortcut()
        runCurrent()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), anyOrNull())
        assertNotNull(captor.firstValue.intent)
        assertEquals(pluginComp, captor.firstValue.launcherPlugin)
    }

    @Test
    fun `should show launch chooser dots only if multiple launch plugins exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock()))

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertTrue(newViewModel.showLaunchChooser.value)
        // If multiple launch plugins exist, we also show the shortcut chooser to allow picking the launch plugin for the shortcut
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should show shortcut chooser dots only if multiple shortcut plugins exist`() {
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(mock()))
        whenever(createShortcutUseCase.getPlugins()).thenReturn(listOf(mock(), mock()))

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertFalse(newViewModel.showLaunchChooser.value)
        assertTrue(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should pre-select plugin when editing shortcut`() {
        val pluginComp = mockComponentName("pkg", "cls")
        val launchPlugin = PluginInfo("Launch Plugin", pluginComp, mockIcon())
        whenever(launchActivityUseCase.getPlugins()).thenReturn(listOf(launchPlugin))

        val shortcutWithPlugin = shortcutRequest.copy(launcherPlugin = pluginComp)
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutWithPlugin,
                "configuration" to DetailsConfiguration.SHORTCUTS,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, favoritesRepository, recentsRepository, toggleFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, shortcutsRepository, settingsRepository, savedStateHandle,
        )

        assertEquals(launchPlugin, newViewModel.selectedLaunchPlugin.value)
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
        val testIcon: IconCompat = mockIcon()
        whenever(iconLoader.tryGetIcon(iconRes)).thenReturn(Result.success(testIcon))

        viewModel.updateIconResourceName(iconRes)
        testDispatcher.scheduler.runCurrent()

        assertEquals(iconRes, viewModel.editedIconResourceName.value)
        assertNull(viewModel.editedIconUri.value)
        assertEquals(testIcon, viewModel.editedIcon.value)
    }

    @Test
    fun `should update icon uri and load icon`() = runTest {
        val uri = mock<android.net.Uri>()
        val testIcon: IconCompat = mockIcon()
        whenever(iconLoader.getIcon(any<android.net.Uri>())).thenReturn(Result.success(testIcon))

        viewModel.updateIconUri(uri)
        testDispatcher.scheduler.runCurrent()

        assertEquals(uri, viewModel.editedIconUri.value)
        assertEquals("", viewModel.editedIconResourceName.value)
        assertEquals(testIcon, viewModel.editedIcon.value)
    }

    @Test
    fun `should update edited icon and load icon`() = runTest {
        val icon = mockIcon()

        viewModel.updateEditedIcon(icon)
        testDispatcher.scheduler.runCurrent()

        assertNull(viewModel.editedIconUri.value)
        assertEquals("", viewModel.editedIconResourceName.value)
        assertNotNull(viewModel.editedIcon.value)
    }

    @Test
    fun `should clear other icon source when updating one`() = runTest {
        val iconRes = "com.test:drawable/icon"
        val uri = mock<android.net.Uri>()
        val resIcon = mockIcon()
        val uriIcon = mockIcon()
        whenever(iconLoader.tryGetIcon(any())).thenReturn(Result.success(resIcon))
        whenever(iconLoader.getIcon(any<android.net.Uri>())).thenReturn(Result.success(uriIcon))

        viewModel.updateIconResourceName(iconRes)
        assertEquals(iconRes, viewModel.editedIconResourceName.value)
        assertNull(viewModel.editedIconUri.value)

        viewModel.updateIconUri(uri)
        assertEquals("", viewModel.editedIconResourceName.value)
        assertEquals(uri, viewModel.editedIconUri.value)

        viewModel.updateIconResourceName(iconRes)
        assertEquals(iconRes, viewModel.editedIconResourceName.value)
        assertNull(viewModel.editedIconUri.value)
    }

    @Test
    fun `should emit error message with debounce when icon loading fails`() = runTest {
        val iconRes = "invalid_icon"
        whenever(iconLoader.tryGetIcon(iconRes)).thenReturn(Result.failure(IconLoader.NullResourceException()))
        val testIcon: IconCompat = mockIcon()
        whenever(getActivityIconUseCase.invoke(null, componentName)).thenReturn(testIcon)

        val errorMessages = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.errorMessage.collect { msg -> errorMessages.add(msg) }
        }

        viewModel.updateIconResourceName(iconRes)

        // Advance time by 2 seconds
        advanceTimeBy(2.seconds)
        runCurrent()

        assertEquals(1, errorMessages.size)
        assertEquals(R.string.error_invalid_icon_resource, errorMessages[0])
        // Fallback happened, so it should be testIcon
        assertEquals(testIcon, viewModel.editedIcon.value)

        job.cancel()
    }

    @Test
    fun `should update canLaunch state based on package and class`() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canLaunch.collect { values.add(it) }
        }

        viewModel.updatePackage("")
        viewModel.updateClass("Class")
        assertFalse(viewModel.canLaunch.value)

        viewModel.updatePackage("pkg")
        viewModel.updateClass("")
        assertFalse(viewModel.canLaunch.value)

        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertTrue(viewModel.canLaunch.value)

        job.cancel()
    }

    @Test
    fun `should update canCreateShortcut state based on name package and class`() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canCreateShortcut.collect { values.add(it) }
        }

        viewModel.updateName("")
        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertFalse(viewModel.canCreateShortcut.value)

        viewModel.updateName("Name")
        viewModel.updatePackage("")
        viewModel.updateClass("Class")
        assertFalse(viewModel.canCreateShortcut.value)

        viewModel.updateName("Name")
        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertTrue(viewModel.canCreateShortcut.value)

        job.cancel()
    }

    @Test
    fun `should update canShare state based on package and class`() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canShare.collect { values.add(it) }
        }

        viewModel.updatePackage("")
        viewModel.updateClass("Class")
        assertFalse(viewModel.canShare.value)

        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertTrue(viewModel.canShare.value)

        job.cancel()
    }

    @Test
    fun `should update canFavorite state based on name package and class`() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.canFavorite.collect { values.add(it) }
        }

        viewModel.updateName("")
        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertFalse(viewModel.canFavorite.value)

        viewModel.updateName("Name")
        viewModel.updatePackage("pkg")
        viewModel.updateClass("Class")
        assertTrue(viewModel.canFavorite.value)

        job.cancel()
    }
}
