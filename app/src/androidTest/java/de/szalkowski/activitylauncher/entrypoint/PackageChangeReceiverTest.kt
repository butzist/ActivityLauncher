package de.szalkowski.activitylauncher.entrypoint

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@dagger.hilt.android.testing.HiltAndroidTest
@dagger.hilt.android.testing.UninstallModules(de.szalkowski.activitylauncher.app.di.CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class PackageChangeReceiverTest {
    @get:Rule
    val hiltRule = dagger.hilt.android.testing.HiltAndroidRule(this)

    private lateinit var receiver: PackageChangeReceiver

    @dagger.hilt.android.testing.BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @dagger.hilt.android.testing.BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @dagger.hilt.android.testing.BindValue
    val intentSigner: de.szalkowski.activitylauncher.domain.launcher.IntentSigner = mock()

    @dagger.hilt.android.testing.BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @dagger.hilt.android.testing.BindValue
    val shortcutCreator: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator = mock()

    @dagger.hilt.android.testing.BindValue
    val activityLauncherProxy: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy = mock()

    @dagger.hilt.android.testing.BindValue
    val shortcutCreatorProxy: de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy = mock()

    @dagger.hilt.android.testing.BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @dagger.hilt.android.testing.BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @dagger.hilt.android.testing.BindValue
    val viewIntentParser: de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser = mock()

    @dagger.hilt.android.testing.BindValue
    val settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository = mock()

    @dagger.hilt.android.testing.BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @dagger.hilt.android.testing.BindValue
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

        // Give some time for the coroutine in receiver to run
        var verified = false
        for (i in 1..10) {
            try {
                verify(packageRepository).loadDetails(packageName)
                verified = true
                break
            } catch (e: Throwable) {
                Thread.sleep(50)
            }
        }
        if (!verified) verify(packageRepository).loadDetails(packageName)
    }

    @Test
    fun testPackageRemovedTriggersRemovePackage() = runBlocking {
        val packageName = "com.test.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REMOVED).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Intent.EXTRA_REPLACING, false)
        }

        receiver.onReceive(context, intent)

        // Give some time for the coroutine in receiver to run
        var verified = false
        for (i in 1..10) {
            try {
                verify(packageRepository).removePackage(packageName)
                verified = true
                break
            } catch (e: Throwable) {
                Thread.sleep(50)
            }
        }
        if (!verified) verify(packageRepository).removePackage(packageName)
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
