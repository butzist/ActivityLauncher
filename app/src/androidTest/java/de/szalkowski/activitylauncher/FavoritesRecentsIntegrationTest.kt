package de.szalkowski.activitylauncher

import android.content.ComponentName
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.domain.model.SystemActivity
import de.szalkowski.activitylauncher.domain.model.SystemPackage
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FavoritesRecentsIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Inject
    lateinit var packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository

    @Before
    fun setup() {
        hiltRule.inject()
        settingsRepository.disclaimerAccepted = true
        systemRepository.clear()

        // Clear shared preferences to ensure a clean state
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("al_recent_activities", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("al_favorites", android.content.Context.MODE_PRIVATE).edit().clear().commit()

        // Setup fake data
        val pkg = SystemPackage("de.szalkowski.activitylauncher", "Android Test App", "1.0 (1)", null)
        val activities = listOf(
            SystemActivity(ComponentName("de.szalkowski.activitylauncher", "de.szalkowski.activitylauncher.entrypoint.MainActivity"), "Main Activity", null, false, isDefault = true),
        )
        systemRepository.addPackage(pkg, activities)
        packageRepository.invalidate()
    }

    @Test
    fun testFavoritesAndRecentsNavigation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
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
            favoriteButton.perform(click())

            Thread.sleep(2000)
            onView(withId(R.id.FavoritesFragment)).perform(click())
            Thread.sleep(2000)
            onView(withId(R.id.rvFavorites)).check(matches(hasMinimumChildCount(1)))
        } finally {
            runCatching { scenario.close() }
        }
    }

    private fun pressBackHome() {
        onView(withId(R.id.bottom_navigation)).perform(click()) // Just a fallback if pressBack fails
    }
}
