package de.szalkowski.activitylauncher

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CreateShortcutIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val disableAnimationsRule = DisableAnimationsRule()

    @Inject
    lateinit var systemRepository: FakeSystemPackageRepository

    @Inject
    lateinit var packageRepository: PackageRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        settingsRepository.disclaimerAccepted = true

        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("al_recent_activities", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("al_favorites", Context.MODE_PRIVATE).edit().clear().commit()

        // Enable using real system package manager to query actual system packages
        systemRepository.useReal = true
        packageRepository.sync()

        val startTime = System.currentTimeMillis()
        while ((!packageRepository.isLoaded) && (System.currentTimeMillis() - startTime < 10000)) {
            Thread.sleep(200)
        }
    }

    @Test
    fun testCreateShortcutAndLaunchFromHomeScreen() {
        val targetPackage = "com.android.settings"
        val shortcutName = "System Settings Shortcut"

        ActivityScenario.launch(MainActivity::class.java).use {
            TestUtils.dismissSystemDialogs()
            TestUtils.waitForWindowFocus()
            Thread.sleep(1000)

            // Search specifically for com.android.settings using the search bar
            onView(withId(R.id.tiSearch))
                .perform(typeText(targetPackage), pressImeActionButton())
            Thread.sleep(1500)

            // Click on the Settings package item in the package list
            onView(withId(R.id.rvPackages))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            Thread.sleep(1500)

            // Click on the first activity item in the activity list
            onView(withId(R.id.rvActivities))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            Thread.sleep(1500)

            // Set a unique name for the shortcut in ActivityDetailsFragment
            onView(withId(R.id.tiName)).perform(replaceText(shortcutName))
            Thread.sleep(500)

            // Click "Create shortcut" in activity details
            onView(withId(R.id.btCreateShortcut)).perform(click())
            Thread.sleep(1000)

            // Confirm pinning shortcut in system dialog
            acceptPinShortcutDialog(device)

            // Press Home to return to launcher
            device.pressHome()
            device.waitForIdle()

            // Find and click the created shortcut on home screen
            val shortcut = findShortcutOnHomeScreen(device, shortcutName)
            assertNotNull("Shortcut with label '$shortcutName' should exist on Home screen", shortcut)
            shortcut?.click()

            // Validate that the Settings app was launched
            TestUtils.waitForWindowFocus(targetPackage, 10000)
            assertEquals(targetPackage, device.currentPackageName)
        }
    }

    private fun acceptPinShortcutDialog(device: UiDevice) {
        Thread.sleep(1000)

        val tosButtonPattern = Pattern.compile("(?i)(accept & continue|accept all|i agree|no thanks|got it)")
        val tosObj = device.findObject(By.text(tosButtonPattern))
        tosObj?.click()

        val pinPattern = Pattern.compile("(?i)(add automatically|add to home screen|add|allow|ok)")
        var pinButton = device.findObject(UiSelector().textMatches(pinPattern.pattern()))
        if (!pinButton.waitForExists(5000)) {
            pinButton = device.findObject(UiSelector().resourceId("android:id/button1"))
        }
        if (pinButton.exists()) {
            pinButton.click()
            device.waitForIdle()
            Thread.sleep(1500)
        }
    }

    private fun findShortcutOnHomeScreen(device: UiDevice, label: String): UiObject2? {
        val launcherPkg = device.launcherPackageName

        fun isLauncherObject(obj: UiObject2): Boolean {
            return obj.applicationPackage != "com.android.systemui" &&
                (launcherPkg == null || obj.applicationPackage == launcherPkg)
        }

        fun searchOnCurrentPage(): UiObject2? {
            val objects = device.findObjects(By.text(label))
            for (obj in objects) {
                if (isLauncherObject(obj)) return obj
            }
            val descObjects = device.findObjects(By.desc(label))
            for (obj in descObjects) {
                if (isLauncherObject(obj)) return obj
            }
            return null
        }

        var obj = searchOnCurrentPage()
        if (obj != null) return obj

        val width = device.displayWidth
        val height = device.displayHeight
        repeat(3) {
            device.swipe(width * 3 / 4, height / 2, width / 4, height / 2, 10)
            device.waitForIdle()
            obj = searchOnCurrentPage()
            if (obj != null) return obj
        }

        repeat(6) {
            device.swipe(width / 4, height / 2, width * 3 / 4, height / 2, 10)
            device.waitForIdle()
            obj = searchOnCurrentPage()
            if (obj != null) return obj
        }

        return null
    }
}
