package de.szalkowski.activitylauncher

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.external.AdManager
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import de.szalkowski.activitylauncher.domain.external.SupportReminder
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.packages.GetPackageIconUseCase
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class, de.szalkowski.activitylauncher.app.di.NoadsModule::class)
@RunWith(AndroidJUnit4::class)
class MainNavigationIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val iconLoader: IconLoader = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val adManager: AdManager = mock()

    @BindValue
    val analyticsLogger: AnalyticsLogger = mock()

    @BindValue
    val activitySharer: ActivitySharer = mock()

    @BindValue
    val supportReminder: SupportReminder = mock()

    @BindValue
    val backupRepository: BackupRepository = mock()

    @BindValue
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @BindValue
    val getPackageIconUseCase: GetPackageIconUseCase = mock()

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(recentsRepository.getRecentsFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(shortcutsRepository.getShortcutsFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isLoaded).thenReturn(true)
        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))
    }

    @Test
    fun testShortcutsTabIsTopLevelDestination() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            // Navigate to Shortcuts
            onView(withId(R.id.ShortcutsFragment)).perform(click())

            // Verify no back button (navigation icon) is shown in the toolbar
            onView(withContentDescription("Navigate up")).check(doesNotExist())
        } finally {
            runCatching { scenario.close() }
        }
    }

    @Test
    fun testRecentsTabIsTopLevelDestination() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            // Navigate to Recents
            onView(withId(R.id.RecentsFragment)).perform(click())

            // Verify no back button is shown
            onView(withContentDescription("Navigate up")).check(doesNotExist())
        } finally {
            runCatching { scenario.close() }
        }
    }
}
