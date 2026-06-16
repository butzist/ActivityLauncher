package de.szalkowski.activitylauncher

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityDetailsIntegrationTest {

    @Test
    fun testActivityDetailsAndFavorites() {
        // Prepare intent to launch ActivityDetailsFragment via MainActivity navigation
        // Note: MainActivity usually starts at PackageListFragment or FavoritesFragment
        val intent = Intent(androidx.test.core.app.ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        // 1. Navigate to ActivityDetails (Assuming we can click a package then an activity)
        // For simplicity in this test, we assume there's at least one package/activity
        onView(withId(R.id.rvPackages)).perform(click())
        // Now in ActivityListFragment, click first activity
        onView(withId(R.id.rvActivities)).perform(click())

        // 2. Test Favorite Toggle
        val favoriteButton = onView(withId(R.id.btFavorite))
        val initialText = getText(favoriteButton)

        favoriteButton.perform(click())

        // Verify text changed (e.g. from "Add to favorites" to "Remove from favorites")
        favoriteButton.check(matches(not(withText(initialText))))

        // Check ActionBar icon (this is harder with Espresso but we can check if it's displayed)
        onView(withId(R.id.action_favorite)).check(matches(isDisplayed()))

        // 3. Test Launch Button (doesn't crash)
        onView(withId(R.id.btLaunch)).perform(click())

        // 4. Test Create Shortcut (doesn't crash)
        onView(withId(R.id.btCreateShortcut)).perform(click())

        scenario.close()
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
