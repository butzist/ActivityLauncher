package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class LaunchActivityUseCaseTest {
    private val activityLauncher: ActivityLauncher = mock()
    private val activityLauncherProxy: ActivityLauncherProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()
    private val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()
    private lateinit var useCase: LaunchActivityUseCase
    private val componentName = mock<ComponentName> {
        on { packageName } doReturn "com.test"
        on { className } doReturn "Activity"
        on { flattenToShortString() } doReturn "com.test/Activity"
    }

    @Before
    fun setup() {
        useCase = LaunchActivityUseCase(activityLauncher, activityLauncherProxy, recentsRepository, packageRepository, getActivityIconUseCase)
    }

    @Test
    fun `should launch activity and add to recents even if not primary`() {
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val info = de.szalkowski.activitylauncher.domain.model.MyActivityInfo(componentName, "Name", null, false)
        whenever(packageRepository.getActivity(any())).thenReturn(info)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(ActivityIcon.Resource("pkg", 1))

        val request = LaunchRequest(intent, source = LaunchSource.PRIMARY)
        useCase.invoke(request)

        verify(activityLauncher).launchActivity(eq(request), anyOrNull())
        verify(recentsRepository).addActivity(any<ShortcutRequest>(), eq(true))
    }

    @Test
    fun `should add to recents if primary request`() {
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val icon = ActivityIcon.Resource("pkg", 1)
        val request = LaunchRequest(intent, name = "Name", icon = icon, source = LaunchSource.PRIMARY)

        useCase.invoke(request)

        verify(recentsRepository).addActivity(any<ShortcutRequest>(), eq(true))
    }

    @Test
    fun `should launch activity with plugin and add to recents`() {
        val plugin = ComponentName("com.plugin", "Plugin")
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val info = de.szalkowski.activitylauncher.domain.model.MyActivityInfo(componentName, "Name", null, false)
        whenever(packageRepository.getActivity(any())).thenReturn(info)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(ActivityIcon.Resource("pkg", 1))

        val request = LaunchRequest(intent, launcherPlugin = plugin, source = LaunchSource.PRIMARY)
        useCase.invoke(request)

        verify(activityLauncherProxy).launchActivity(eq(request), anyOrNull())
        verify(recentsRepository).addActivity(any<ShortcutRequest>(), eq(true))
    }

    @Test
    fun `should add to recents without metadata update if from shortcut`() {
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val info = de.szalkowski.activitylauncher.domain.model.MyActivityInfo(componentName, "Name", null, false)
        whenever(packageRepository.getActivity(any())).thenReturn(info)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(ActivityIcon.Resource("pkg", 1))

        val request = LaunchRequest(intent, source = LaunchSource.SHORTCUT)
        useCase.invoke(request)

        verify(activityLauncher).launchActivity(eq(request), anyOrNull())
        verify(recentsRepository).addActivity(any<ShortcutRequest>(), eq(false))
    }

    @Test
    fun `should check for multiple handlers`() {
        whenever(activityLauncherProxy.hasMultipleHandlers()).thenReturn(true)
        assertTrue(useCase.hasMultipleHandlers())

        whenever(activityLauncherProxy.hasMultipleHandlers()).thenReturn(false)
        assertFalse(useCase.hasMultipleHandlers())
    }

    @Test
    fun `toShortcutRequest should resolve metadata correctly`() {
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val request = LaunchRequest(intent, source = LaunchSource.PRIMARY)
        val info = de.szalkowski.activitylauncher.domain.model.MyActivityInfo(componentName, "Resolved Name", "icon_res", false)
        whenever(packageRepository.getActivity(any())).thenReturn(info)
        val mockIcon = ActivityIcon.Resource("pkg", 1)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(mockIcon)

        val shortcutRequest = request.toShortcutRequest(packageRepository, getActivityIconUseCase)

        assertEquals("Resolved Name", shortcutRequest.name)
        assertEquals(mockIcon, shortcutRequest.icon)
        assertEquals(intent, shortcutRequest.intent)
        assertEquals(LaunchSource.PRIMARY, shortcutRequest.source)
    }

    @Test
    fun `toShortcutRequest should preserve custom metadata`() {
        val intent = mock<Intent> {
            on { component } doReturn componentName
        }
        val customIcon = ActivityIcon.Resource("pkg", 2)
        val request = LaunchRequest(intent, name = "Custom Name", icon = customIcon, source = LaunchSource.SAVED)

        val shortcutRequest = request.toShortcutRequest(packageRepository, getActivityIconUseCase)

        assertEquals("Custom Name", shortcutRequest.name)
        assertEquals(customIcon, shortcutRequest.icon)
        assertEquals(LaunchSource.SAVED, shortcutRequest.source)
    }

    @Test
    fun `toLaunchRequest should copy metadata correctly`() {
        val intent = mock<Intent>()
        val icon = ActivityIcon.Resource("pkg", 1)
        val plugin = mock<ComponentName>()
        val request = ShortcutRequest("Name", intent, icon, plugin, source = LaunchSource.SAVED)

        val launchRequest = request.toLaunchRequest()

        assertEquals("Name", launchRequest.name)
        assertSame(intent, launchRequest.intent)
        assertSame(icon, launchRequest.icon)
        assertSame(plugin, launchRequest.launcherPlugin)
        assertEquals(LaunchSource.SAVED, launchRequest.source)
    }

    @Test
    fun `toLaunchRequest should override source if provided`() {
        val request = ShortcutRequest("Name", mock(), ActivityIcon.Resource("pkg", 1), source = LaunchSource.SAVED)

        val launchRequest = request.toLaunchRequest(LaunchSource.PRIMARY)

        assertEquals(LaunchSource.PRIMARY, launchRequest.source)
    }
}
