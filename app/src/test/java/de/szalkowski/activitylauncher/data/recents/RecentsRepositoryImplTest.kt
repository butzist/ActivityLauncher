package de.szalkowski.activitylauncher.data.recents

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import de.szalkowski.activitylauncher.data.database.RecentDao
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecentsRepositoryImplTest {
    private val context: Context = mock()
    private val recentDao: RecentDao = mock()
    private val packageRepository: PackageRepository = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: RecentsRepositoryImpl

    private val componentName = mock<ComponentName> {
        on { packageName } doReturn "com.test"
        on { className } doReturn "Activity"
        on { flattenToString() } doReturn "com.test/Activity"
    }
    private lateinit var icon: ActivityIcon
    private lateinit var mockIntent: Intent
    private lateinit var request: ShortcutRequest

    @Before
    fun setup() {
        icon = ActivityIcon.Resource("pkg", 1)

        mockIntent = mock<Intent> {
            on { component } doReturn componentName
            on { toUri(any()) } doReturn "intent:#Intent;component=com.test/Activity;end"
        }
        request = ShortcutRequest("Test Activity", mockIntent, icon, source = LaunchSource.PRIMARY)

        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.getBoolean(any(), any())).thenReturn(true) // migration done
        whenever(prefs.getStringSet(any(), any())).thenReturn(emptySet())
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenReturn(editor)

        repository = RecentsRepositoryImpl(context, recentDao, packageRepository, getActivityIconUseCase, testDispatcher)
    }

    @Test
    fun `addActivity with updateMetadata=true should always insert`() = runTest(testDispatcher) {
        repository.addActivity(request, updateMetadata = true)

        verify(recentDao).insert(any())
        verify(recentDao, never()).updateUsage(any(), any(), any(), anyOrNull(), any())
    }

    @Test
    fun `addActivity with updateMetadata=false should try updateUsage first`() = runTest(testDispatcher) {
        whenever(recentDao.updateUsage(any(), any(), any(), anyOrNull(), any())).thenReturn(1)

        repository.addActivity(request, updateMetadata = false)

        verify(recentDao).updateUsage(any(), any(), any(), anyOrNull(), any())
        verify(recentDao, never()).insert(any())
    }

    @Test
    fun `addActivity with updateMetadata=false should insert if updateUsage fails`() = runTest(testDispatcher) {
        whenever(recentDao.updateUsage(any(), any(), any(), anyOrNull(), any())).thenReturn(0)

        repository.addActivity(request, updateMetadata = false)

        verify(recentDao).updateUsage(any(), any(), any(), anyOrNull(), any())
        verify(recentDao).insert(any())
    }
}
