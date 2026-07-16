package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val shortcutCreator: ShortcutCreator = mock()
    private val shortcutCreatorProxy: ShortcutCreatorProxy = mock()
    private val componentName = mock<ComponentName> {
        on { packageName } doReturn "com.test"
        on { className } doReturn "Activity"
        on { flattenToShortString() } doReturn "com.test/Activity"
    }
    private val icon = mock<androidx.core.graphics.drawable.IconCompat>()
    private val intent = mock<android.content.Intent> {
        on { component } doReturn componentName
    }
    private val request = ShortcutRequest("Test", intent, icon)
    private val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()
    private val shortcutsRepository: de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository = mock()
    private lateinit var useCase: CreateShortcutUseCase

    @Before
    fun setup() {
        useCase = CreateShortcutUseCase(shortcutCreator, shortcutCreatorProxy, recentsRepository, shortcutsRepository)
        runBlocking {
            whenever(shortcutsRepository.recordShortcut(any())).thenReturn(123L)
        }
    }

    @Test
    fun `should add activity to recents and record shortcut`() {
        runBlocking {
            useCase(request)
            verify(recentsRepository).addActivity(eq(request))
            verify(shortcutsRepository).recordShortcut(eq(request))
        }
    }

    @Test
    fun `should use shortcutCreator if only one handler exists`() {
        runBlocking {
            whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(false)

            useCase(request)

            verify(shortcutCreator).createLauncherIcon(eq(request), eq(123L))
            verify(shortcutCreatorProxy, never()).createLauncherIcon(any(), anyOrNull(), anyOrNull())
        }
    }

    @Test
    fun `should use shortcutCreatorProxy if multiple handlers exist`() {
        runBlocking {
            whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(true)

            useCase(request)

            verify(shortcutCreatorProxy).createLauncherIcon(eq(request), isNull(), eq(123L))
            verify(shortcutCreator, never()).createLauncherIcon(any(), anyOrNull())
        }
    }

    @Test
    fun `should use shortcutCreatorProxy if plugin is provided`() {
        runBlocking {
            val plugin = ComponentName("com.plugin", "Plugin")
            useCase(request, shortcutPlugin = plugin)

            verify(shortcutCreatorProxy).createLauncherIcon(eq(request), eq(plugin), eq(123L))
            verify(shortcutCreator, never()).createLauncherIcon(any(), anyOrNull())
        }
    }
}
