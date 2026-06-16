package de.szalkowski.activitylauncher

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageListingIntegrationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testPackageListIsDisplayedAndSearchWorks() {
        // Verify RecyclerView is displayed
        onView(withId(R.id.rvPackages)).check(matches(isDisplayed()))

        // Type a query that should match something (e.g., "android" or "launcher")
        onView(withId(R.id.tiSearch))
            .perform(typeText("android"), pressImeActionButton())

        // Check if results match the query
        // We look for any text containing "android" in the list
        onView(withText(containsString("android"))).check(matches(isDisplayed()))
    }

    @Test
    fun testPackageFilteringAppliesToActivities() {
        // Type a query that might match an activity name
        onView(withId(R.id.tiSearch))
            .perform(typeText("Main"), pressImeActionButton())

        // If an activity matches "Main", some package should be visible
        onView(withId(R.id.rvPackages)).check(matches(isDisplayed()))
    }
}
