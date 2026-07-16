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
import de.szalkowski.activitylauncher.app.di.FeatureServicesModule
import de.szalkowski.activitylauncher.data.launcher.ViewIntentParserImpl
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.external.AdManager
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import de.szalkowski.activitylauncher.domain.external.SupportReminder
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class, FeatureServicesModule::class)
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
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val activitySharer: ActivitySharer = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val packageRepository: PackageRepository = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val backupRepository: BackupRepository = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @Before
    fun init() {
        hiltRule.inject()
        val icon = ActivityIcon.Resource(ApplicationProvider.getApplicationContext<android.content.Context>().packageName, android.R.drawable.sym_def_app_icon)
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(icon)

        val realParser = ViewIntentParserImpl()
        whenever(viewIntentParser.parseLaunchRequest(any(), anyOrNull())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock ->
            realParser.parseLaunchRequest(invocation.getArgument(0), invocation.getArgument(1))
        }
        whenever(viewIntentParser.parseShortcutRequest(any())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock -> realParser.parseShortcutRequest(invocation.getArgument(0)) }
        whenever(viewIntentParser.componentNameFromIntent(any())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock -> realParser.componentNameFromIntent(invocation.getArgument(0)) }
        whenever(viewIntentParser.parseShortcutIntent(any())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock -> realParser.parseShortcutIntent(invocation.getArgument(0)) }
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(supportReminder.shouldDisplayReminder()).thenReturn(false)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(recentsRepository.getRecentsFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))

        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(value = false))
        whenever(packageRepository.isLoaded).thenReturn(true)

        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val componentName = invocation.getArgument<ComponentName>(0)
            de.szalkowski.activitylauncher.domain.model.MyActivityInfo(
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
                val shortcutRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    args?.getParcelable("shortcutRequest", ShortcutRequest::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    args?.getParcelable<ShortcutRequest>("shortcutRequest")
                }
                assertEquals("com.android.settings", shortcutRequest?.intent?.component?.packageName)
                assertEquals("com.android.settings.Settings", shortcutRequest?.intent?.component?.className)

                verify(recentsRepository).addActivity(eq(shortcutRequest!!), any())
            }
        }
    }

    @Test
    fun testDeepLinkBackstack() {
        val packageName = "com.android.settings"
        val className = ".Settings"
        val componentName = ComponentName(packageName, "com.android.settings$className")

        // Mock a favorite to change the default start destination
        whenever(favoritesRepository.getFavorites()).thenReturn(setOf(componentName))

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://activitylauncher.net/activity/$packageName/$className")
            setClassName(ApplicationProvider.getApplicationContext(), MainActivity::class.java.name)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)

                // Should be at Details
                assertEquals(R.id.ActivityDetailsFragment, navController.currentDestination?.id)

                verify(recentsRepository).addActivity(any(), any())

                // Navigate back to ActivityListFragment
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.ActivityListFragment, navController.currentDestination?.id)
                assertEquals(packageName, navController.currentBackStackEntry?.arguments?.getString("packageName"))

                // Navigate back to PackageListFragment
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.PackageListFragment, navController.currentDestination?.id)

                // Navigate back to FavoritesFragment (root)
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.FavoritesFragment, navController.currentDestination?.id)
            }
        }
    }

    @Test
    fun testShortcutRedirectBackstack() {
        val packageName = "com.test"
        val className = "com.test.Activity"
        val componentName = ComponentName(packageName, className)

        // Mock a favorite to change the default start destination
        whenever(favoritesRepository.getFavorites()).thenReturn(setOf(componentName))

        val launchIntent = Intent().apply {
            component = componentName
        }
        val launchRequest = LaunchRequest(launchIntent, source = LaunchSource.SHORTCUT)
        val intent = Intent().apply {
            putExtra(MainActivity.EXTRA_SHORTCUT_LAUNCH_REDIRECT, launchRequest)
            setClassName(ApplicationProvider.getApplicationContext(), MainActivity::class.java.name)
        }

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)

                // Should be at Details
                assertEquals(R.id.ActivityDetailsFragment, navController.currentDestination?.id)

                verify(recentsRepository).addActivity(any(), any())

                // Navigate back to ActivityListFragment
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.ActivityListFragment, navController.currentDestination?.id)
                assertEquals(packageName, navController.currentBackStackEntry?.arguments?.getString("packageName"))

                // Navigate back to PackageListFragment
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.PackageListFragment, navController.currentDestination?.id)

                // Navigate back to FavoritesFragment (root)
                activity.onBackPressedDispatcher.onBackPressed()
                assertEquals(R.id.FavoritesFragment, navController.currentDestination?.id)
            }
        }
    }
}
