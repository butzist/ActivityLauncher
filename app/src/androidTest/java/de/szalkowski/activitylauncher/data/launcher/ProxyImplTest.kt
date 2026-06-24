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
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
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
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

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
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @Inject
    @ApplicationContext
    lateinit var context: Context

    private lateinit var proxy: ShortcutCreatorProxyImpl
    private val packageManager: PackageManager = mock()

    @Before
    fun init() {
        hiltRule.inject()
        // We can't easily mock the context provided by Hilt, but we can wrap it or use it.
        // Actually, ShortcutCreatorProxyImpl uses @ApplicationContext Context, so we'll use a real one
        // and mock the PackageManager it returns if possible, or just mock the whole context.
        val mockContext: Context = mock()
        whenever(mockContext.packageManager).thenReturn(packageManager)
        proxy = ShortcutCreatorProxyImpl(mockContext, getActivityIconUseCase, intentSigner)
    }

    @Test
    fun testCreateLauncherIconDelegation() {
        val componentName = ComponentName("com.test", "Activity")
        val activityInfo = SystemActivity(componentName, "Test", null, false)
        val icon: IconCompat = mock()
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(icon)
        whenever(intentSigner.signIntent(any(), anyOrNull())).thenReturn("signature")

        // Mock the context and capture it to verify startActivity
        val mockContext: Context = mock()
        whenever(mockContext.packageManager).thenReturn(packageManager)
        val proxyWithMockContext = ShortcutCreatorProxyImpl(mockContext, getActivityIconUseCase, intentSigner)

        proxyWithMockContext.createLauncherIcon(activityInfo)

        verify(getActivityIconUseCase).invoke(isNull(), eq(componentName))
        verify(intentSigner).signIntent(any<Intent>(), isNull<String>())
        verify(mockContext).startActivity(any<Intent>())
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
