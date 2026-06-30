package de.szalkowski.activitylauncher

import android.content.ComponentName
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.packages.GetPackageIconUseCase
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class PackageListingIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

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
    val activitySharer: ActivitySharer = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @BindValue
    val getPackageIconUseCase: GetPackageIconUseCase = mock()

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))

        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(ApplicationProvider.getApplicationContext(), android.R.drawable.sym_def_app_icon)
        whenever(getPackageIconUseCase(anyOrNull(), any())).thenReturn(icon)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(icon)

        systemRepository.clear()

        // Clear shared preferences to ensure a clean state
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("al_recent_activities", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("al_favorites", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        // Setup fake data
        val pkg = SystemPackage("de.szalkowski.activitylauncher", "Android Test App", "1.0 (1)", null)
        val activities = listOf(
            MyActivityInfo(ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity"), "Main Activity", null, false, isDefault = true),
            MyActivityInfo(ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.SettingsActivity"), "Settings Activity", null, false),
        )
        systemRepository.addPackage(pkg, activities)

        val activityNames = activities.map {
            de.szalkowski.activitylauncher.domain.model.ActivityName(
                name = it.name,
                shortCls = it.componentName.className.substringAfterLast('.'),
                fullCls = it.componentName.className,
                isPrivate = it.isPrivate,
                iconResourceName = it.iconResourceName,
            )
        }

        val myPackageInfo = de.szalkowski.activitylauncher.domain.model.MyPackageInfo(
            id = 1L,
            packageName = pkg.packageName,
            name = pkg.name,
            version = pkg.version,
            defaultActivityName = activityNames[0],
            activityNames = activityNames,
            iconResourceName = pkg.iconResourceName,
        )
        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(listOf(myPackageInfo)))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))

        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val componentName = invocation.getArgument<ComponentName>(0)
            activities.find { it.componentName == componentName } ?: activities[0]
        }
        whenever(packageRepository.getActivities(any())).thenAnswer { invocation ->
            val packageName = invocation.getArgument<String>(0)
            de.szalkowski.activitylauncher.domain.model.PackageActivities(packageName, pkg.name, activities[0], activities)
        }
    }

    @Test
    fun testPackageListIsDisplayedAndSearchWorks() {
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(5000)
            onView(withId(R.id.PackageListFragment)).perform(click())
            Thread.sleep(2000)
            onView(withId(R.id.rvPackages)).check(matches(isDisplayed()))

            onView(withId(R.id.tiSearch))
                .perform(typeText("android"), pressImeActionButton())

            Thread.sleep(2000)
            onView(withId(R.id.rvPackages)).check(matches(hasMinimumChildCount(1)))
        }
    }

    @Test
    fun testPackageFilteringAppliesToActivities() {
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(5000)
            onView(withId(R.id.PackageListFragment)).perform(click())
            Thread.sleep(2000)
            onView(withId(R.id.tiSearch))
                .perform(typeText("Settings"), pressImeActionButton())

            Thread.sleep(2000)
            onView(withId(R.id.rvPackages)).check(matches(isDisplayed()))
        }
    }
}
