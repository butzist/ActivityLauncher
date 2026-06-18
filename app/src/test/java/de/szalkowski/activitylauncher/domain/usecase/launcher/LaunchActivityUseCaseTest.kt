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
    private val context: android.content.Context = mock()
    private val activityLauncher: ActivityLauncherProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: LaunchActivityUseCase
    private val componentName = ComponentName("com.test", "Activity")

    @Before
    fun setup() {
        whenever(context.getText(any())).thenReturn("Test")
        useCase = LaunchActivityUseCase(context, activityLauncher, recentsRepository)
    }

    @Test
    fun `should launch activity and add to recents`() {
        useCase.invoke(componentName, showToast = false, useChooser = false)

        verify(activityLauncher).launchActivity(componentName, useChooser = false)
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should launch activity with chooser and add to recents`() {
        useCase.invoke(componentName, showToast = false, useChooser = true)

        verify(activityLauncher).launchActivity(componentName, useChooser = true)
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
