package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.runBlocking
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
        proxy = ShortcutCreatorProxyImpl(mockContext, intentSigner, shortcutsRepository)
    }

    @Test
    fun testCreateLauncherIconDelegation() {
        val componentName = ComponentName("com.test", "Activity")
        val icon: IconCompat = mock()
        whenever(intentSigner.signRequest(any<ShortcutRequest>())).thenReturn("signature")
        runBlocking { whenever(shortcutsRepository.recordShortcut(any())).thenReturn(1L) }

        // Mock the context and capture it to verify startActivity
        val mockContext: Context = mock()
        whenever(mockContext.packageManager).thenReturn(packageManager)
        whenever(mockContext.packageName).thenReturn("de.szalkowski.activitylauncher")
        val proxyWithMockContext = ShortcutCreatorProxyImpl(mockContext, intentSigner, shortcutsRepository)

        val request = ShortcutRequest("Test", Intent().setComponent(componentName), icon)
        runBlocking { proxyWithMockContext.createLauncherIcon(request, null, 1L) }

        verify(intentSigner).signRequest(eq(request))
        verify(mockContext).startActivity(any<Intent>())
        runBlocking { verify(shortcutsRepository, never()).recordShortcut(any()) }
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
