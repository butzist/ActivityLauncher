package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class LaunchActivityUseCaseTest {
    private val activityLauncher: ActivityLauncherProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: LaunchActivityUseCase
    private val componentName = ComponentName("com.test", "Activity")

    @Before
    fun setup() {
        useCase = LaunchActivityUseCase(activityLauncher, recentsRepository)
    }

    @Test
    fun `should launch activity and add to recents`() {
        useCase.invoke(componentName)

        verify(activityLauncher).launchActivity(componentName, plugin = null)
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should launch activity with plugin and add to recents`() {
        val plugin = ComponentName("com.plugin", "Plugin")
        useCase.invoke(componentName, launchPlugin = plugin)

        verify(activityLauncher).launchActivity(componentName, plugin = plugin)
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should check for multiple handlers`() {
        whenever(activityLauncher.hasMultipleHandlers()).thenReturn(true)
        assertTrue(useCase.hasMultipleHandlers())

        whenever(activityLauncher.hasMultipleHandlers()).thenReturn(false)
        assertFalse(useCase.hasMultipleHandlers())
    }
}
