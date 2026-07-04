package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ActivityLauncherImplTest {
    private val context: Context = mock()
    private lateinit var activityLauncher: ActivityLauncher

    @Before
    fun setup() {
        activityLauncher = ActivityLauncherImpl(context)
    }

    @Test
    fun testLaunchActivityWithExtras() {
        val componentName = ComponentName("com.android.settings", "com.android.settings.Settings")
        val extras = Bundle().apply {
            putString("test_key", "test_value")
        }
        val intent = Intent().apply {
            component = componentName
            putExtras(extras)
        }
        val request = LaunchRequest(intent)

        activityLauncher.launchActivity(request)

        argumentCaptor<Intent>().apply {
            verify(context).startActivity(capture())
            val capturedIntent = firstValue
            assertEquals(componentName, capturedIntent.component)
            assertEquals("test_value", capturedIntent.getStringExtra("test_key"))
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
