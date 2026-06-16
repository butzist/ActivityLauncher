package de.szalkowski.activitylauncher

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesRecentsIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testFavoritesAndRecentsNavigation() {
        // 1. Navigate to Favorites
        onView(withId(R.id.FavoritesFragment)).perform(click())
        onView(withId(R.id.rvFavorites)).check(matches(isDisplayed()))

        // 2. Navigate to Recents
        onView(withId(R.id.RecentsFragment)).perform(click())
        onView(withId(R.id.rvRecents)).check(matches(isDisplayed()))
    }

    @Test
    fun testFavoriteToggleUpdatesUI() {
        // Navigate to an activity detail (from Package List)
        onView(withId(R.id.PackageListFragment)).perform(click())
        onView(withId(R.id.rvPackages)).perform(click())
        onView(withId(R.id.rvActivities)).perform(click())

        // Get initial state
        val favoriteButton = onView(withId(R.id.btFavorite))

        // Toggle Favorite
        favoriteButton.perform(click())

        // Verify it's now in Favorites fragment
        onView(withId(R.id.FavoritesFragment)).perform(click())
        // Assuming the list was empty or we can find our item
        onView(withId(R.id.rvFavorites)).check(matches(hasMinimumChildCount(1)))
    }
}
