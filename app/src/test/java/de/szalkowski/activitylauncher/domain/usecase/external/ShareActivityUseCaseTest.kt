package de.szalkowski.activitylauncher.domain.usecase.external

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ShareActivityUseCaseTest {
    private val activitySharer: ActivitySharer = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: ShareActivityUseCase
    private val componentName = ComponentName("com.test", "Activity")

    @Before
    fun setup() {
        useCase = ShareActivityUseCase(activitySharer, recentsRepository)
    }

    @Test
    fun `should share activity`() {
        useCase.invoke(componentName)

        verify(activitySharer).shareActivity(componentName)
    }
}
