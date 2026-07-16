package de.szalkowski.activitylauncher.data.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
class ProxyImplTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy = mock()

    @BindValue
    val activityLauncherProxy: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy = mock()

    @BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    val viewIntentParser: de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser = mock()

    @BindValue
    val settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository = mock()

    @BindValue
    val backupRepository: BackupRepository = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private lateinit var proxy: ShortcutCreatorProxyImpl
    private val packageManager: PackageManager = mock()

    @Before
    fun init() {
        hiltRule.inject()
        val mockContext: Context = mock()
        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(mockContext.packageName).thenReturn("de.szalkowski.activitylauncher")
        proxy = ShortcutCreatorProxyImpl(mockContext)
    }

    @Test
    fun testCreateLauncherIconDelegation() {
        val icon = ActivityIcon.Resource("de.szalkowski.activitylauncher", 123)

        // Mock the context and capture it to verify startActivity
        val mockContext: Context = mock()
        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(mockContext.packageName).thenReturn("de.szalkowski.activitylauncher")
        val proxyWithMockContext = ShortcutCreatorProxyImpl(mockContext)

        val request = ShortcutProxyRequest("Test", icon, Intent("action.TEST"), source = LaunchSource.PRIMARY)
        runBlocking { proxyWithMockContext.createLauncherIcon(request, null) }

        argumentCaptor<Intent>().apply {
            verify(mockContext).startActivity(capture())
            val capturedIntent = firstValue
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun testCreateLauncherIconWithActivityContextDoesNotAddFlag() {
        val icon = ActivityIcon.Resource("de.szalkowski.activitylauncher", 123)

        val activityContext: Activity = mock()
        whenever(activityContext.packageManager).thenReturn(packageManager)
        whenever(activityContext.packageName).thenReturn("de.szalkowski.activitylauncher")
        val proxyWithActivityContext = ShortcutCreatorProxyImpl(activityContext)

        val request = ShortcutProxyRequest("Test", icon, Intent("action.TEST"), source = LaunchSource.PRIMARY)
        runBlocking { proxyWithActivityContext.createLauncherIcon(request, null, activityContext) }

        argumentCaptor<Intent>().apply {
            verify(activityContext).startActivity(capture())
            val capturedIntent = firstValue
            assertEquals(0, capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun testHasMultipleHandlers() {
        val resolveInfo = mock<ResolveInfo>()
        whenever(packageManager.queryIntentActivities(any(), any<Int>())).thenReturn(listOf(resolveInfo, resolveInfo))

        assertTrue(proxy.hasMultipleHandlers())

        whenever(packageManager.queryIntentActivities(any(), any<Int>())).thenReturn(listOf(resolveInfo))
        assertFalse(proxy.hasMultipleHandlers())
    }
}
