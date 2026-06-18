package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val context: android.content.Context = mock()
    private val shortcutCreator: ShortcutCreatorProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: CreateShortcutUseCase

    private val componentName = ComponentName("com.test", "Activity")
    private val activityInfo = MyActivityInfo(componentName, "Test", null, false)

    @Before
    fun setup() {
        whenever(context.getText(any())).thenReturn("Test")
        useCase = CreateShortcutUseCase(context, shortcutCreator, recentsRepository)
    }

    @Test
    fun `should create shortcut and add to recents`() {
        useCase.invoke(activityInfo, null, showToast = false, useChooser = false)

        verify(shortcutCreator).createLauncherIcon(eq(activityInfo), isNull(), eq(false))
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should create shortcut with chooser and add to recents`() {
        useCase.invoke(activityInfo, null, showToast = false, useChooser = true)

        verify(shortcutCreator).createLauncherIcon(eq(activityInfo), isNull(), eq(true))
        verify(recentsRepository).addActivity(componentName)
    }

    @Test
    fun `should check for multiple handlers`() {
        whenever(shortcutCreator.hasMultipleHandlers()).thenReturn(true)
        assertTrue(useCase.hasMultipleHandlers())

        whenever(shortcutCreator.hasMultipleHandlers()).thenReturn(false)
        assertFalse(useCase.hasMultipleHandlers())
    }
}
