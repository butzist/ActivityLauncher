package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class LaunchActivityUseCaseTest {
    private val activityLauncher: ActivityLauncher = mock()
    private val activityLauncherProxy: ActivityLauncherProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: LaunchActivityUseCase
    private val componentName = ComponentName("com.test", "Activity")

    @Before
    fun setup() {
        useCase = LaunchActivityUseCase(activityLauncher, activityLauncherProxy, recentsRepository)
    }

    @Test
    fun `should launch activity and add to recents`() {
        val request = LaunchRequest(componentName)
        useCase.invoke(request)

        verify(activityLauncher).launchActivity(eq(request))
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should launch activity with plugin and add to recents`() {
        val plugin = ComponentName("com.plugin", "Plugin")
        val request = LaunchRequest(componentName)
        useCase.invoke(request, launchPlugin = plugin)

        verify(activityLauncherProxy).launchActivity(eq(request), eq(plugin))
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should check for multiple handlers`() {
        whenever(activityLauncherProxy.hasMultipleHandlers()).thenReturn(true)
        assertTrue(useCase.hasMultipleHandlers())

        whenever(activityLauncherProxy.hasMultipleHandlers()).thenReturn(false)
        assertFalse(useCase.hasMultipleHandlers())
    }
}
