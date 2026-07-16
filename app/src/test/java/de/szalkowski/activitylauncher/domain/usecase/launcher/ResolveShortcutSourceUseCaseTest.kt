package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ResolveShortcutSourceUseCaseTest {
    private val shortcutsRepository: ShortcutsRepository = mock()
    private val intentSigner: IntentSigner = mock()
    private val packageRepository: PackageRepository = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private lateinit var useCase: ResolveShortcutSourceUseCase

    @Before
    fun setup() {
        useCase = ResolveShortcutSourceUseCase(shortcutsRepository, intentSigner, packageRepository, getActivityIconUseCase)
    }

    @Test
    fun `should resolve from repository if id exists`() = runTest {
        val id = "uuid-1"
        val intent = mock<Intent>()
        val shortcutRequest = ShortcutRequest("Name", intent, ActivityIcon.Resource("pkg", 1), source = LaunchSource.SAVED)
        val managedShortcut = ShortcutsRepository.ManagedShortcut(id, shortcutRequest, 0L)
        whenever(shortcutsRepository.getShortcut(id)).thenReturn(managedShortcut)

        val source = ShortcutSource(id = id, intent = null, signature = null, launcherPlugin = null)
        val result = useCase(source)

        assertTrue(result is ShortcutResolutionResult.Success)
        assertEquals(intent, (result as ShortcutResolutionResult.Success).request.intent)
    }

    @Test
    fun `should resolve from signature and restore if id missing in repository`() = runTest {
        val id = "uuid-1"
        val intent = mock<Intent> {
            on { component } doReturn ComponentName("com.test", "Activity")
        }
        val signature = "valid_signature"
        val source = ShortcutSource(id = id, intent = intent, signature = signature, launcherPlugin = null)

        whenever(shortcutsRepository.getShortcut(id)).thenReturn(null)
        whenever(intentSigner.validateRequestSignature(any(), eq(signature))).thenReturn(true)

        val info = MyActivityInfo(ComponentName("com.test", "Activity"), "Resolved Name", null, isPrivate = false)
        whenever(packageRepository.getActivity(any())).thenReturn(info)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(ActivityIcon.Resource("pkg", 1))

        val result = useCase(source)

        assertTrue(result is ShortcutResolutionResult.Success)
        verify(shortcutsRepository).recordShortcut(any(), eq(id))
    }

    @Test
    fun `should return InvalidSignature if signature validation fails`() = runTest {
        val intent = mock<Intent>()
        val signature = "invalid"
        val source = ShortcutSource(id = null, intent = intent, signature = signature, launcherPlugin = null)

        whenever(intentSigner.validateRequestSignature(any(), eq(signature))).thenReturn(false)

        val result = useCase(source)

        assertEquals(ShortcutResolutionResult.InvalidSignature, result)
        verify(shortcutsRepository, never()).recordShortcut(any(), anyOrNull())
    }

    @Test
    fun `should return NotFound if neither id nor signature works`() = runTest {
        val source = ShortcutSource(id = "not-found", intent = null, signature = null, launcherPlugin = null)
        whenever(shortcutsRepository.getShortcut("not-found")).thenReturn(null)

        val result = useCase(source)

        assertEquals(ShortcutResolutionResult.NotFound, result)
    }
}
