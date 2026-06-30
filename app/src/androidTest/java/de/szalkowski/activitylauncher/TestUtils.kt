package de.szalkowski.activitylauncher

import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

object TestUtils {
    private const val TAG = "TestUtils"

    fun dismissSystemDialogs() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Ensure keyboard is hidden
        try {
            Espresso.closeSoftKeyboard()
        } catch (e: Exception) {
            Log.d(TAG, "Keyboard already closed or not shown")
        }

        // Ensure screen is on
        if (!device.isScreenOn) {
            device.wakeUp()
        }

        // Common buttons in system dialogs or initial app dialogs
        val commonButtons = listOf(
            "Cancel",
            "Dismiss",
            "Don't add",
            "No",
            "Close",
            "OK",
            "Accept",
            "Allow",
        )

        val regex = "(?i)" + commonButtons.joinToString("|")

        repeat(3) {
            val button = device.findObject(UiSelector().textMatches(regex))
            if (button.exists()) {
                Log.d(TAG, "Dismissing dialog with button: ${button.text}")
                button.click()
                Thread.sleep(1000)
            }
        }
    }

    fun waitForWindowFocus(timeoutMs: Long = 10000) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (device.currentPackageName != "com.android.systemui" && device.currentPackageName != null) {
                // If we are not in system UI, we likely have focus or are in our app
                return
            }
            Thread.sleep(500)
            Log.d(TAG, "Waiting for window focus... current package: ${device.currentPackageName}")
        }
    }
}
