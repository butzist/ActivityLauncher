package de.szalkowski.activitylauncher

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * JUnit rule that captures a screenshot if a test fails.
 * Screenshots are saved to /sdcard/screenshots/ with a filename based on the test class and name.
 */
class ScreenshotTestRule : TestWatcher() {
    private val tag = "ScreenshotTestRule"
    private val screenshotDir = "/storage/emulated/0/screenshots/"

    override fun failed(e: Throwable?, description: Description) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val fileName = "${description.className}_${description.methodName}.png"
        val screenshotFile = File(screenshotDir, fileName)

        try {
            // Ensure the directory exists
            device.executeShellCommand("mkdir -p $screenshotDir")

            if (device.takeScreenshot(screenshotFile)) {
                Log.i(tag, "Screenshot saved to: ${screenshotFile.absolutePath}")
            } else {
                Log.e(tag, "Failed to capture screenshot")
            }
        } catch (ex: Exception) {
            Log.e(tag, "Error saving screenshot", ex)
        }
    }
}
