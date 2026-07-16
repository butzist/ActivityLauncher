package de.szalkowski.activitylauncher.presentation.favorites

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val favoritesRepository: FavoritesRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(MutableStateFlow(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load favorites`() = runTest {
        val component = ComponentName("com.test", "Activity")
        val mockIntent: Intent = mock()
        whenever(mockIntent.component).thenReturn(component)
        whenever(mockIntent.toUri(any())).thenReturn("intent:#Intent;component=com.test/Activity;end")

        val request = ShortcutRequest("Activity", mockIntent, ActivityIcon.Resource("pkg", 1), source = LaunchSource.SAVED)
        val flow = MutableStateFlow(listOf(request))

        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(flow)

        val viewModel = FavoritesViewModel(favoritesRepository)
        viewModel.setDispatcher(testDispatcher)

        // Collect flow to trigger updates
        val items = mutableListOf<List<ShortcutRequest>>()
        val job = launch {
            viewModel.items.collect { items.add(it) }
        }

        // Wait for flow collection in BaseShortcutListViewModel init
        runCurrent()

        assertEquals(1, viewModel.items.value.size)
        assertEquals("Activity", viewModel.items.value[0].name)

        job.cancel()
    }

    @Test
    fun `handleSaveResult should add favorite`() = runTest {
        val viewModel = FavoritesViewModel(favoritesRepository)
        val request: ShortcutRequest = mock()
        val result = DetailsResult.Save(request, null)

        viewModel.handleSaveResult(result)

        verify(favoritesRepository).addFavorite(request)
    }
}
