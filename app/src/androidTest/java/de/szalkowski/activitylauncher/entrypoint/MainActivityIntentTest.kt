package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.app.di.NoadsModule
import de.szalkowski.activitylauncher.data.launcher.ViewIntentParserImpl
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.external.AdManager
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import de.szalkowski.activitylauncher.domain.external.SupportReminder
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class, NoadsModule::class)
@RunWith(AndroidJUnit4::class)
class MainActivityIntentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val adManager: AdManager = mock()

    @BindValue
    val analyticsLogger: AnalyticsLogger = mock()

    @BindValue
    val supportReminder: SupportReminder = mock()

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val iconLoader: IconLoader = mock()

    @BindValue
    val activitySharer: ActivitySharer = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    // Using real implementation for parser to test logic
    @BindValue
    val viewIntentParser: ViewIntentParser = ViewIntentParserImpl()

    @Before
    fun init() {
        hiltRule.inject()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(supportReminder.shouldDisplayReminder()).thenReturn(false)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())

        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))

        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val componentName = invocation.getArgument<ComponentName>(0)
            de.szalkowski.activitylauncher.domain.model.SystemActivity(
                componentName,
                "Test Activity",
                null,
                false,
            )
        }

        whenever(packageRepository.getActivities(any())).thenAnswer { invocation ->
            val packageName = invocation.getArgument<String>(0)
            de.szalkowski.activitylauncher.domain.model.PackageActivities(
                packageName,
                "Test App",
                null,
                emptyList(),
            )
        }
    }

    @Test
    fun testDeepLinkNavigationToDetails() {
        val componentName = "com.android.settings/.Settings"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://activitylauncher.net/activity/$componentName")
            setClassName(ApplicationProvider.getApplicationContext(), MainActivity::class.java.name)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
                assertEquals(R.id.ActivityDetailsFragment, navController.currentDestination?.id)
                val args = navController.currentBackStackEntry?.arguments
                val navComponentName = args?.getParcelable<ComponentName>("activityComponentName")
                assertEquals("com.android.settings", navComponentName?.packageName)
                assertEquals("com.android.settings.Settings", navComponentName?.className)
            }
        }
    }

    @Test
    fun testDeepLinkBackstack() {
        val packageName = "com.android.settings"
        val className = ".Settings"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://activitylauncher.net/activity/$packageName/$className")
            setClassName(ApplicationProvider.getApplicationContext(), MainActivity::class.java.name)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)

                // Should be at Details
                assertEquals(R.id.ActivityDetailsFragment, navController.currentDestination?.id)

                // Navigate back - this is where we expect it to go to ActivityListFragment if correctly implemented
                activity.onBackPressedDispatcher.onBackPressed()

                // Verify it went to ActivityListFragment
                assertEquals(R.id.ActivityListFragment, navController.currentDestination?.id)
                assertEquals(packageName, navController.currentBackStackEntry?.arguments?.getString("packageName"))

                // Navigate back again
                activity.onBackPressedDispatcher.onBackPressed()

                // Should be at Package List
                assertEquals(R.id.PackageListFragment, navController.currentDestination?.id)
            }
        }
    }

    @Test
    fun testShortcutIntentNavigationToDetails() {
        val launchIntent = Intent().apply {
            component = ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            setClassName(ApplicationProvider.getApplicationContext(), MainActivity::class.java.name)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
                assertEquals(R.id.ActivityDetailsFragment, navController.currentDestination?.id)
            }
        }
    }
}
