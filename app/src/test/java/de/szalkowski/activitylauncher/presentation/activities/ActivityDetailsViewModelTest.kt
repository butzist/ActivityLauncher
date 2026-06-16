package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.ShareActivityUseCase
import de.szalkowski.activitylauncher.domain.usecase.favorites.ToggleFavoriteUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailsViewModelTest {
    private val activityRepository: ActivityRepository = mock()
    private val favoritesRepository: FavoritesRepository = mock()
    private val launchActivityUseCase: LaunchActivityUseCase = mock()
    private val createShortcutUseCase: CreateShortcutUseCase = mock()
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = mock()
    private val shareActivityUseCase: ShareActivityUseCase = mock()
    private val iconLoader: IconLoader = mock()
    private val recentsRepository: RecentsRepository = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val componentName: ComponentName = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ActivityDetailsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        whenever(componentName.packageName).thenReturn("com.test")
        whenever(componentName.className).thenReturn("com.test.Activity")
        whenever(componentName.flattenToShortString()).thenReturn("com.test/.Activity")

        runTest {
            val activityInfo = MyActivityInfo(
                componentName,
                "Test Activity",
                mock<Drawable>(),
                "res:icon",
                false,
            )
            whenever(activityRepository.getActivity(any())).thenReturn(activityInfo)
            whenever(favoritesRepository.isFavorite(any())).thenReturn(false)
        }

        val savedStateHandle = SavedStateHandle(mapOf("activityComponentName" to componentName))
        viewModel = ActivityDetailsViewModel(
            activityRepository, favoritesRepository, launchActivityUseCase,
            createShortcutUseCase, toggleFavoriteUseCase, shareActivityUseCase,
            iconLoader, recentsRepository, settingsRepository, savedStateHandle,
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
        viewModel.launchActivity(asRoot = false)
        verify(launchActivityUseCase).invoke(any(), eq(false), any())
    }

    @Test
    fun `should launch activity as root`() {
        viewModel.launchActivity(asRoot = true)
        verify(launchActivityUseCase).invoke(any(), eq(true), any())
    }

    @Test
    fun `should create shortcut`() {
        viewModel.createShortcut(asRoot = false)
        verify(createShortcutUseCase).invoke(any(), eq(false), anyOrNull())
    }

    @Test
    fun `should share activity`() {
        viewModel.shareActivity()
        verify(shareActivityUseCase).invoke(any())
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
}
