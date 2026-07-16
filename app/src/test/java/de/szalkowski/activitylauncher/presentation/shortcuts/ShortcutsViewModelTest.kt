package de.szalkowski.activitylauncher.presentation.shortcuts

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutsViewModelTest {
    private val shortcutsRepository: ShortcutsRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(shortcutsRepository.getShortcutsFlow()).thenReturn(emptyFlow())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleSaveResult should record new shortcut`() = runTest {
        val viewModel = ShortcutsViewModel(shortcutsRepository)
        val request: ShortcutRequest = mock()
        val result = DetailsResult.Save(request, null)

        viewModel.handleSaveResult(result)

        verify(shortcutsRepository).recordShortcut(request)
    }

    @Test
    fun `handleSaveResult should update existing shortcut`() = runTest {
        val viewModel = ShortcutsViewModel(shortcutsRepository)
        val request: ShortcutRequest = mock()
        val shortcutId = "uuid-123"
        val result = DetailsResult.Save(request, shortcutId)

        viewModel.handleSaveResult(result)

        verify(shortcutsRepository).updateShortcut(shortcutId, request)
    }
}
