package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ShortcutCreatorImplTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val activityLauncherProxy: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreatorProxy: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy = mock()

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

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    lateinit var shortcutCreator: ShortcutCreator

    private lateinit var shortcutCreatorImpl: ShortcutCreatorImpl
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun init() {
        shortcutCreatorImpl = ShortcutCreatorImpl(context, intentSigner)
        shortcutCreator = shortcutCreatorImpl
        hiltRule.inject()
    }

    @Test
    fun testCreateLauncherIcon() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)

        val componentName = ComponentName("com.test", "com.test.Activity")
        val icon = androidx.core.graphics.drawable.IconCompat.createWithBitmap(
            android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888),
        )
        val signature = "test_signature"
        whenever(intentSigner.signRequest(any())).thenReturn(signature)

        val shortcutManager = mock<ShortcutManager>()
        val mockContext = object : android.content.ContextWrapper(context) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.SHORTCUT_SERVICE) return shortcutManager
                return super.getSystemService(name)
            }

            override fun getSystemServiceName(serviceClass: Class<*>): String? {
                if (serviceClass == ShortcutManager::class.java) return Context.SHORTCUT_SERVICE
                return super.getSystemServiceName(serviceClass)
            }
        }

        val shortcutCreatorWithMock = ShortcutCreatorImpl(mockContext, intentSigner)
        val request = ShortcutRequest("Test App", componentName, icon)

        shortcutCreatorWithMock.createLauncherIcon(request)

        val shortcutCaptor = argumentCaptor<android.content.pm.ShortcutInfo>()
        verify(shortcutManager).requestPinShortcut(shortcutCaptor.capture(), isNull())

        val capturedShortcut = shortcutCaptor.firstValue
        assertEquals("Test App", capturedShortcut.shortLabel)
        val intent = capturedShortcut.intent
        org.junit.Assert.assertNotNull(intent)
        assertEquals(ShortcutCreator.INTENT_LAUNCH_SHORTCUT, intent?.action)
        assertEquals(ShortcutActivity::class.java.name, intent?.component?.className)
        assertEquals(signature, intent?.getStringExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE))

        val launchIntentUri = intent?.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
        val launchIntent = Intent.parseUri(launchIntentUri, Intent.URI_INTENT_SCHEME)
        assertEquals(componentName, launchIntent.component)
    }
}
