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
        } catch (_: Exception) {
            Log.d(TAG, "Keyboard already closed or not shown")
        }

        // Close system dialogs via intent
        try {
            device.executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        } catch (_: Exception) {
            Log.d(TAG, "Failed to broadcast CLOSE_SYSTEM_DIALOGS")
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
            "Accept all",
            "Allow",
            "Wait",
            "No thanks",
            "I agree",
            "Got it",
            "Continue",
        )

        val regex = "(?i)" + commonButtons.joinToString("|")

        repeat(3) {
            val button = device.findObject(UiSelector().textMatches(regex))
            if (button.exists()) {
                Log.d(TAG, "Dismissing dialog with button: ${button.text}")
                button.click()
                device.waitForIdle()
            }
        }
    }

    fun waitForWindowFocus(packageName: String? = null, timeoutMs: Long = 10000) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val targetPackage = packageName ?: InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentPackage = device.currentPackageName
            if (currentPackage == targetPackage) {
                return
            }
            Thread.sleep(500)
            Log.d(TAG, "Waiting for window focus for $targetPackage... current package: $currentPackage")
        }
    }
}
