package de.szalkowski.activitylauncher.data.launcher

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
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutProxyRequest
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import kotlinx.coroutines.runBlocking
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
    val backupRepository: BackupRepository = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val shortcutUpdateConfirmation: de.szalkowski.activitylauncher.domain.launcher.ShortcutUpdateConfirmation = mock()

    @BindValue
    lateinit var shortcutCreator: ShortcutCreator

    private lateinit var shortcutCreatorImpl: ShortcutCreatorImpl
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun init() {
        shortcutCreatorImpl = ShortcutCreatorImpl(context, shortcutUpdateConfirmation)
        shortcutCreator = shortcutCreatorImpl
        hiltRule.inject()
    }

    @Test
    fun testCreateLauncherIcon() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)

        val icon = ActivityIcon.Resource(context.packageName, android.R.drawable.sym_def_app_icon)
        val testIntent = Intent("action.TEST")

        val shortcutManager = mock<ShortcutManager>()
        val mockContext = object : android.content.ContextWrapper(context) {
            override fun getSystemService(name: String): Any? {
                if (name == SHORTCUT_SERVICE) return shortcutManager
                return super.getSystemService(name)
            }

            override fun getSystemServiceName(serviceClass: Class<*>): String? {
                if (serviceClass == ShortcutManager::class.java) return Context.SHORTCUT_SERVICE
                return super.getSystemServiceName(serviceClass)
            }
        }

        val shortcutCreatorWithMock = ShortcutCreatorImpl(mockContext, shortcutUpdateConfirmation)
        val request = ShortcutProxyRequest("Test App", icon, testIntent, source = LaunchSource.PRIMARY)

        runBlocking { shortcutCreatorWithMock.createLauncherIcon(request, "uuid-123", mockContext) }

        val shortcutCaptor = argumentCaptor<android.content.pm.ShortcutInfo>()
        verify(shortcutManager).requestPinShortcut(shortcutCaptor.capture(), isNull())

        val capturedShortcut = shortcutCaptor.firstValue
        assertEquals("Test App", capturedShortcut.shortLabel)
        assertEquals("uuid-123", capturedShortcut.id)
        val intent = capturedShortcut.intent
        org.junit.Assert.assertNotNull(intent)
        assertEquals("action.TEST", intent?.action)
    }
}
