package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
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
    private val icon = ActivityIcon.Resource("pkg", 1)
    private val intent = mock<android.content.Intent> {
        on { component } doReturn componentName
    }
    private val request = ShortcutRequest("Test", intent, icon, source = LaunchSource.PRIMARY)
    private val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()
    private val shortcutsRepository: de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository = mock()
    private val createShortcutIntentUseCase: CreateShortcutIntentUseCase = mock()
    private lateinit var useCase: CreateShortcutUseCase

    @Before
    fun setup() {
        useCase = CreateShortcutUseCase(shortcutCreator, shortcutCreatorProxy, recentsRepository, shortcutsRepository, createShortcutIntentUseCase)
        runBlocking {
            whenever(shortcutsRepository.recordShortcut(any(), anyOrNull())).thenReturn("uuid-123")
            whenever(createShortcutIntentUseCase.invoke(any(), any())).thenReturn(mock())
        }
    }

    @Test
    fun `should add activity to recents and record shortcut`() {
        runBlocking {
            useCase(request)
            verify(recentsRepository).addActivity(eq(request), any())
            verify(shortcutsRepository).recordShortcut(eq(request), isNull())
        }
    }

    @Test
    fun `should use shortcutCreator if only one handler exists`() {
        runBlocking {
            whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(false)

            useCase(request)

            verify(shortcutCreator).createLauncherIcon(any(), eq("uuid-123"), anyOrNull())
            verify(shortcutCreatorProxy, never()).createLauncherIcon(any(), anyOrNull(), anyOrNull())
        }
    }

    @Test
    fun `should use shortcutCreatorProxy if multiple handlers exist`() {
        runBlocking {
            whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(true)

            useCase(request)

            verify(shortcutCreatorProxy).createLauncherIcon(any(), isNull(), anyOrNull())
            verify(shortcutCreator, never()).createLauncherIcon(any(), any(), anyOrNull())
        }
    }

    @Test
    fun `should use shortcutCreatorProxy if plugin is provided`() {
        runBlocking {
            val plugin = ComponentName("com.plugin", "Plugin")
            useCase(request, shortcutPlugin = plugin)

            verify(shortcutCreatorProxy).createLauncherIcon(any(), eq(plugin), anyOrNull())
            verify(shortcutCreator, never()).createLauncherIcon(any(), any(), anyOrNull())
        }
    }
}
