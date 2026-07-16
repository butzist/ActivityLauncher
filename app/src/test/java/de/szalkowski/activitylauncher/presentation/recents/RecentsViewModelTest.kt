package de.szalkowski.activitylauncher.presentation.recents

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
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
class RecentsViewModelTest {
    private val recentsRepository: RecentsRepository = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        whenever(recentsRepository.getRecentsFlow()).thenReturn(emptyFlow())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleSaveResult should add activity`() = runTest {
        val viewModel = RecentsViewModel(recentsRepository)
        val request: ShortcutRequest = mock()
        val result = DetailsResult.Save(request, null)

        viewModel.handleSaveResult(result)

        verify(recentsRepository).addActivity(request)
    }
}
