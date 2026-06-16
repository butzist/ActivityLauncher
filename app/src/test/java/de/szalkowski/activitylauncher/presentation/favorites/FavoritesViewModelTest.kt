package de.szalkowski.activitylauncher.presentation.favorites

import android.content.ComponentName
import android.graphics.drawable.Drawable
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.packages.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val favoritesRepository: FavoritesRepository = mock()
    private val activityRepository: ActivityRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load favorites`() = runTest {
        val component = ComponentName("com.test", "Activity")
        val activityInfo = MyActivityInfo(component, "Activity", mock<Drawable>(), null, false)

        whenever(favoritesRepository.getFavorites()).thenReturn(setOf(component))
        whenever(activityRepository.getActivity(component)).thenReturn(activityInfo)

        val viewModel = FavoritesViewModel(favoritesRepository, activityRepository)
        viewModel.setDispatcher(testDispatcher)
        viewModel.load()
        advanceUntilIdle()

        assertEquals(1, viewModel.activities.value.size)
        assertEquals("Activity", viewModel.activities.value[0].name)
    }
}
