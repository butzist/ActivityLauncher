package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val shortcutCreator: ShortcutCreator = mock()
    private val shortcutCreatorProxy: ShortcutCreatorProxy = mock()
    private val componentName = ComponentName("com.test", "Activity")
    private val activityInfo = SystemActivity(componentName, "Test", null, false)
    private lateinit var useCase: CreateShortcutUseCase

    @Before
    fun setup() {
        useCase = CreateShortcutUseCase(shortcutCreator, shortcutCreatorProxy)
    }

    @Test
    fun `should use shortcutCreator if only one handler exists`() {
        whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(false)

        useCase(activityInfo)

        verify(shortcutCreator).createLauncherIcon(eq(activityInfo), isNull(), eq(false))
        verify(shortcutCreatorProxy, never()).createLauncherIcon(any<SystemActivity>(), anyOrNull(), any<Boolean>())
    }

    @Test
    fun `should use shortcutCreatorProxy if multiple handlers exist`() {
        whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(true)

        useCase(activityInfo)

        verify(shortcutCreatorProxy).createLauncherIcon(eq(activityInfo), isNull(), eq(false))
        verify(shortcutCreator, never()).createLauncherIcon(any<SystemActivity>(), anyOrNull(), any<Boolean>())
    }
}
