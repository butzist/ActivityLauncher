package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getActivityIntentFromIntentDef
import de.szalkowski.activitylauncher.core.util.getIntentDefFromActivityIntent
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.GetIsFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.ToggleFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
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
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()
    private val getIsFavoriteUseCase: GetIsFavoriteUseCase = mock()
    private val launchActivityUseCase: LaunchActivityUseCase = mock()
    private val createShortcutUseCase: CreateShortcutUseCase = mock()
    private val shareActivityUseCase: ShareActivityUseCase = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val iconLoader: IconLoader = mock()
    private val settingsRepository: SettingsRepository = mock()
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

    private val mockIcon: ActivityIcon = mockIcon()
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

    private fun mockIcon(): ActivityIcon {
        return ActivityIcon.Resource("com.test", 1)
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
        }.thenReturn(IntentDef())

        shortcutRequest = ShortcutRequest("Test Activity", mockIntent, mockIcon, source = LaunchSource.PRIMARY)

        val defaultIcon = mockIcon()
        whenever(packageRepository.getActivity(any())).thenReturn(activityInfo)
        whenever(getIsFavoriteUseCase.invoke(any())).thenReturn(false)
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
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

        val captor = argumentCaptor<ShortcutRequest>()
        verify(toggleFavoriteUseCase).invoke(captor.capture())
        val request = captor.firstValue
        assertEquals("Test Activity", request.name)
        assertEquals(mockIntent, request.intent)
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

        val context: Context = mock()
        viewModel.launchActivity(context)
        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture(), eq(context))
        val request = captor.firstValue
        assertEquals("Test Activity", request.name)
        assertEquals(launchIntent, request.intent)
        assertEquals(mockIcon, request.icon)
    }

    @Test
    fun `should create shortcut`() = runTest {
        val shortcutIntent = mockIntent("shortcutIntent")
        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(shortcutIntent)

        val context: Context = mock()
        viewModel.createShortcut(context)
        runCurrent()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), isNull(), anyOrNull(), eq(context))
        val request = captor.firstValue
        assertEquals("Test Activity", request.name)
        assertEquals(shortcutIntent, request.intent)
        assertEquals(mockIcon, request.icon)
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
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
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        assertFalse(newViewModel.showLaunchChooser.value)
        assertFalse(newViewModel.showShortcutChooser.value)
    }

    @Test
    fun `should emit save result`() = runTest {
        val shortcutId = "uuid-123"
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "shortcutId" to shortcutId,
                "configuration" to DetailsConfiguration.SHORTCUTS,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        mockedUtil.`when`<Intent> {
            getActivityIntentFromIntentDef(anyOrNull(), anyOrNull())
        }.thenReturn(mockIntent)

        val results = mutableListOf<DetailsResult>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            newViewModel.onResult.collect { results.add(it) }
        }

        val newName = "Modified Name"
        newViewModel.updateName(newName)
        newViewModel.saveShortcut()
        runCurrent()

        assertEquals(1, results.size)
        val result = results[0] as DetailsResult.Save
        assertEquals(shortcutId, result.id)
        assertEquals(newName, result.request.name)
        job.cancel()
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)
        newViewModel.launchActivity()

        val captor = argumentCaptor<LaunchRequest>()
        verify(launchActivityUseCase).invoke(captor.capture(), anyOrNull())
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectShortcutPlugin(pluginComp)
        newViewModel.createShortcut()
        runCurrent()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(
            request = captor.capture(),
            shortcutId = isNull(),
            shortcutPlugin = eq(pluginComp),
            context = anyOrNull(),
        )
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
        )

        newViewModel.selectLaunchPlugin(pluginComp)
        newViewModel.createShortcut()
        runCurrent()

        val captor = argumentCaptor<ShortcutRequest>()
        verify(createShortcutUseCase).invoke(captor.capture(), isNull(), anyOrNull(), anyOrNull())
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
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

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "shortcutRequest" to shortcutRequest,
                "configuration" to DetailsConfiguration.ALL,
            ),
        )
        val newViewModel = ActivityDetailsViewModel(
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
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
            packageRepository, toggleFavoriteUseCase, getIsFavoriteUseCase,
            launchActivityUseCase, createShortcutUseCase, shareActivityUseCase,
            getActivityIconUseCase, iconLoader, settingsRepository, savedStateHandle,
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
        val testIcon: ActivityIcon = mockIcon()
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
        val testIcon: ActivityIcon = mockIcon()
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
        val testIcon: ActivityIcon = mockIcon()
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
