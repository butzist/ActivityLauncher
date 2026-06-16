package de.szalkowski.activitylauncher

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic smoke test to verify that the app can be installed and launched.
 * This is useful for catching crashes that occur during application startup
 * or activity creation (like the baseline profile or MultiDex issues).
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @Test
    fun testAppLaunches() {
        // Launch the activity. This will trigger the Application.onCreate()
        // and MainActivity.onCreate() methods.
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            // Verify that the activity was created successfully
            assertNotNull(activity)
        }

        // Close the scenario to release resources
        scenario.close()
    }
}
