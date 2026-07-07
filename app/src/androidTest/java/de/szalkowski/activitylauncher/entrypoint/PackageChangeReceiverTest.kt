package de.szalkowski.activitylauncher.entrypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(de.szalkowski.activitylauncher.app.di.CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class PackageChangeReceiverTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var receiver: PackageChangeReceiver

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    val intentSigner: de.szalkowski.activitylauncher.domain.launcher.IntentSigner = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val shortcutCreator: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator = mock()

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

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        hiltRule.inject()
        receiver = PackageChangeReceiver()
    }

    @Test
    fun testPackageAddedTriggersLoadDetails() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = Uri.parse("package:$packageName")
        }

        receiver.onReceive(context, intent)

        repeat(10) {
            try {
                verify(packageRepository).loadDetails(packageName)
                return@runBlocking
            } catch (_: Throwable) {
                Thread.sleep(50)
            }
        }
        verify(packageRepository).loadDetails(packageName)
    }

    @Test
    fun testPackageRemovedTriggersRemovePackage() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_REPLACING, false)
        }

        receiver.onReceive(context, intent)

        repeat(10) {
            try {
                verify(packageRepository).removePackage(packageName)
                return@runBlocking
            } catch (_: Throwable) {
                Thread.sleep(50)
            }
        }
        verify(packageRepository).removePackage(packageName)
    }

    @Test
    fun testPackageRemovedWithReplacingDoesNotTriggerRemove() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_REPLACING, true)
        }

        receiver.onReceive(context, intent)

        verify(packageRepository, never()).removePackage(packageName)
    }
}
