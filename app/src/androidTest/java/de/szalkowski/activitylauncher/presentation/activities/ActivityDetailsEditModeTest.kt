package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.packages.GetPackageIconUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ActivityDetailsEditModeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val iconLoader: IconLoader = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val backupRepository: de.szalkowski.activitylauncher.domain.settings.BackupRepository = mock()

    @BindValue
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @BindValue
    val getPackageIconUseCase: GetPackageIconUseCase = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))
        whenever(packageRepository.isLoaded).thenReturn(true)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.mipmap.sym_def_app_icon)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(icon)
    }

    @Test
    fun testEditModeButtons() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val request = ShortcutRequest("Test", Intent().setComponent(ComponentName("com.test", "Activity")), mock())

        // Pass shortcutId != 0 to trigger edit mode
        val args = bundleOf("shortcutRequest" to request, "shortcutId" to 1L)

        // We use ActivityScenario to launch MainActivity which contains the NavHost
        // But for testing a specific fragment with args, it's easier to use FragmentScenario if possible,
        // or just navigate to it in MainActivity.

        // Since I'm using Hilt, launching the fragment directly with FragmentScenario is cleaner.
        // But Hilt requires a HiltTestActivity.
    }
}
