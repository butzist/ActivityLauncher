package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val shortcutCreator: ShortcutCreator = mock()
    private val shortcutCreatorProxy: ShortcutCreatorProxy = mock()
    private val componentName = ComponentName("com.test", "Activity")
    private val icon = mock<androidx.core.graphics.drawable.IconCompat>()
    private val request = ShortcutRequest("Test", componentName, icon)
    private lateinit var useCase: CreateShortcutUseCase

    @Before
    fun setup() {
        useCase = CreateShortcutUseCase(shortcutCreator, shortcutCreatorProxy)
    }

    @Test
    fun `should use shortcutCreator if only one handler exists`() {
        whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(false)

        useCase(request)

        verify(shortcutCreator).createLauncherIcon(eq(request))
        verify(shortcutCreatorProxy, never()).createLauncherIcon(any(), anyOrNull())
    }

    @Test
    fun `should use shortcutCreatorProxy if multiple handlers exist`() {
        whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(true)

        useCase(request)

        verify(shortcutCreatorProxy).createLauncherIcon(eq(request), isNull())
        verify(shortcutCreator, never()).createLauncherIcon(any())
    }

    @Test
    fun `should use shortcutCreatorProxy if plugin is provided`() {
        val plugin = ComponentName("com.plugin", "Plugin")
        useCase(request, shortcutPlugin = plugin)

        verify(shortcutCreatorProxy).createLauncherIcon(eq(request), eq(plugin))
        verify(shortcutCreator, never()).createLauncherIcon(any())
    }
}
