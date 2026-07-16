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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.external.ActivitySharer
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
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
class FavoritesRecentsIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val screenshotTestRule = ScreenshotTestRule()

    @get:Rule
    val disableAnimationsRule = DisableAnimationsRule()

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
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @BindValue
    val getPackageIconUseCase: GetPackageIconUseCase = mock()

    private val favoriteSet = mutableSetOf<ComponentName>()
    private val favoriteFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ShortcutRequest>>(emptyList())
    private val recentsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ShortcutRequest>>(emptyList())

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Before
    fun setup() {
        hiltRule.inject()
        favoriteSet.clear()
        favoriteFlow.value = emptyList()
        recentsFlow.value = emptyList()

        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavorites()).thenReturn(favoriteSet)
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(favoriteFlow)
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(recentsRepository.getRecentsFlow()).thenReturn(recentsFlow)
        whenever(shortcutsRepository.getShortcutsFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))

        whenever(favoritesRepository.isFavorite(any<ComponentName>())).thenAnswer { invocation ->
            favoriteSet.contains(invocation.getArgument(0))
        }
        doAnswer { invocation ->
            val component = invocation.getArgument<ComponentName>(0)
            if (favoriteSet.add(component)) {
                val icon = getActivityIconUseCase(null, component)
                val intent = Intent().setComponent(component)
                val request = ShortcutRequest("Test Activity", intent, icon)
                favoriteFlow.value += request
            }
        }.whenever(favoritesRepository).addFavorite(any<ComponentName>())

        doAnswer { invocation ->
            val request = invocation.getArgument<ShortcutRequest>(0)
            val component = request.intent.component
            if (component != null && (favoriteSet.add(component))) {
                favoriteFlow.value += request
            }
        }.whenever(favoritesRepository).addFavorite(any<ShortcutRequest>())

        doAnswer { invocation ->
            val component = invocation.getArgument<ComponentName>(0)
            if (favoriteSet.remove(component)) {
                favoriteFlow.value = favoriteFlow.value.filter { it.intent.component != component }
            }
        }.whenever(favoritesRepository).removeFavorite(any<ComponentName>())

        doAnswer { invocation ->
            val request = invocation.getArgument<ShortcutRequest>(0)
            val component = request.intent.component
            if (component != null && favoriteSet.remove(component)) {
                favoriteFlow.value = favoriteFlow.value.filter { it.intent.component != component }
            }
        }.whenever(favoritesRepository).removeFavorite(any<ShortcutRequest>())

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
            MyActivityInfo(ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity"), "Main Activity", null, isPrivate = false, isDefault = true),
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
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(value = false))
        whenever(packageRepository.isLoaded).thenReturn(true)

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
    fun testFavoritesAndRecentsNavigation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            Thread.sleep(5000)
            onView(withId(R.id.PackageListFragment)).perform(click())
            Thread.sleep(2000)

            onView(withId(R.id.FavoritesFragment)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.rvFavorites)).check(matches(isDisplayed()))

            onView(withId(R.id.RecentsFragment)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.rvRecents)).check(matches(isDisplayed()))
        } finally {
            runCatching { scenario.close() }
        }
    }

    @Test
    fun testFavoriteToggleUpdatesUI() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            Thread.sleep(5000)
            onView(withId(R.id.PackageListFragment)).perform(click())

            onView(withId(R.id.rvPackages))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)
            onView(withId(R.id.rvActivities))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)
            val favoriteButton = onView(withId(R.id.btFavorite))
            favoriteButton.perform(scrollTo(), click())

            Thread.sleep(2000)
            onView(withId(R.id.FavoritesFragment)).perform(click())
            Thread.sleep(2000)
            onView(withId(R.id.rvFavorites)).check(matches(hasMinimumChildCount(1)))
        } finally {
            runCatching { scenario.close() }
        }
    }

    @Test
    fun testFavoritesShortClickLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            Thread.sleep(5000)
            onView(withId(R.id.FavoritesFragment)).perform(click())

            // Add a favorite to click on
            val componentName = ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity")
            favoritesRepository.addFavorite(componentName)
            Thread.sleep(2000)

            // Short click
            onView(withId(R.id.rvFavorites))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)
            verify(activityLauncher).launchActivity(any())
        } finally {
            runCatching { scenario.close() }
        }
    }

    @Test
    fun testRecentsShortClickLaunches() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        TestUtils.dismissSystemDialogs()
        TestUtils.waitForWindowFocus()
        try {
            // Mock a recent activity
            val componentName = ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity")
            val icon = getActivityIconUseCase(null, componentName)
            val request = ShortcutRequest("Test Recent", Intent().setComponent(componentName), icon)
            recentsFlow.value = listOf(request)
            Thread.sleep(2000)

            onView(withId(R.id.RecentsFragment)).perform(click())
            Thread.sleep(2000)

            // Short click
            onView(withId(R.id.rvRecents))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

            Thread.sleep(2000)
            verify(activityLauncher).launchActivity(any())
        } finally {
            runCatching { scenario.close() }
        }
    }
}
