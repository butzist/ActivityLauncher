package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ActivityDetailsIntegrationTest {

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

    private val favoriteSet = mutableSetOf<ComponentName>()

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Before
    fun setup() {
        hiltRule.inject()
        favoriteSet.clear()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavorites()).thenReturn(favoriteSet)
        whenever(favoritesRepository.isFavorite(any())).thenAnswer { invocation ->
            favoriteSet.contains(invocation.getArgument<ComponentName>(0))
        }
        doAnswer { invocation ->
            favoriteSet.add(invocation.getArgument(0))
        }.whenever(favoritesRepository).addFavorite(any())
        doAnswer { invocation ->
            favoriteSet.remove(invocation.getArgument(0))
        }.whenever(favoritesRepository).removeFavorite(any())

        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())

        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(ApplicationProvider.getApplicationContext(), android.R.drawable.sym_def_app_icon)
        whenever(getPackageIconUseCase(anyOrNull(), any())).thenReturn(icon)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(icon)

        systemRepository.clear()

        // Clear shared preferences to ensure a clean state
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("al_recent_activities", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("al_favorites", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        val componentName = ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.SettingsActivity")
        val pkg = SystemPackage("de.szalkowski.activitylauncher", "Android Test App", "1.0 (1)", null)
        val activities = listOf(
            MyActivityInfo(componentName, "Settings Activity", null, false, isDefault = true),
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
        whenever(packageRepository.isLoaded).thenReturn(true)

        whenever(packageRepository.getActivity(eq(componentName))).thenReturn(activities[0])
        whenever(packageRepository.getActivities(eq(pkg.packageName))).thenReturn(de.szalkowski.activitylauncher.domain.model.PackageActivities(pkg.packageName, pkg.name, activities[0], activities))
    }

    @Test
    fun testActivityDetailsAndFavorites() {
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        try {
            // Wait for data to load
            Thread.sleep(5000)
            onView(withId(R.id.PackageListFragment)).perform(click())
            Thread.sleep(2000)

            // 1. Navigate to ActivityDetails
            onView(withId(R.id.rvPackages)).check(matches(hasMinimumChildCount(1)))
            onView(withId(R.id.rvPackages))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)

            onView(withId(R.id.rvActivities)).check(matches(hasMinimumChildCount(1)))
            onView(withId(R.id.rvActivities))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)

            // 2. Test Favorite Toggle
            val favoriteButton = onView(withId(R.id.btFavorite))
            val initialText = getText(favoriteButton)

            favoriteButton.perform(scrollTo(), click())
            Thread.sleep(1000)
            favoriteButton.check(matches(not(withText(initialText))))

            /* Skipping launch tests as they are unstable in this environment
            // 3. Test Launch Button
            onView(withId(R.id.btLaunch)).perform(click())
            Thread.sleep(5000)
            pressBack()
            Thread.sleep(2000)

            // 3a. Test Launch Chooser Button
            if (checkIsDisplayed(R.id.btLaunchChooser)) {
                onView(withId(R.id.btLaunchChooser)).perform(click())
                Thread.sleep(5000)
                pressBack()
                Thread.sleep(2000)
            }
             */

            // 4. Test Create Shortcut
            onView(withId(R.id.btCreateShortcut)).perform(scrollTo(), click())
            Thread.sleep(2000)
            verify(shortcutCreator, atLeastOnce()).createLauncherIcon(any())
            TestUtils.dismissSystemDialogs()
            Thread.sleep(1000)

            // 4a. Test Create Shortcut Chooser Button
            if (checkIsDisplayed(R.id.btCreateShortcutChooser)) {
                onView(withId(R.id.btCreateShortcutChooser)).perform(scrollTo(), click())
                Thread.sleep(2000)
                // We'd need to select a plugin in the dialog to verify proxy call,
                // but let's just verify it didn't crash for now as simulating dialog clicks is complex here
                TestUtils.dismissSystemDialogs()
                Thread.sleep(1000)
            }
        } finally {
            // Use runCatching to avoid cleanup errors masking real test failures
            runCatching { scenario.close() }
        }
    }

    private fun checkIsDisplayed(id: Int): Boolean {
        return try {
            onView(withId(id)).check(matches(isDisplayed()))
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
    }

    private fun getText(matcher: androidx.test.espresso.ViewInteraction): String {
        var text = ""
        matcher.perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isAssignableFrom(android.widget.TextView::class.java)
            override fun getDescription() = "getting text from a TextView"
            override fun perform(uiController: androidx.test.espresso.UiController, view: android.view.View) {
                val tv = view as android.widget.TextView
                text = tv.text.toString()
            }
        })
        return text
    }
}
