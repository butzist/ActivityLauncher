package de.szalkowski.activitylauncher.data.launcher

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ActivityLauncherProxyImplTest {
    private val context: Context = mock {
        on { packageManager } doReturn mock()
    }
    private lateinit var proxy: ActivityLauncherProxy

    @Before
    fun setup() {
        proxy = ActivityLauncherProxyImpl(context)
    }

    @Test
    fun testLaunchActivityWithApplicationContextAddsFlag() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val intent = Intent().setComponent(componentName)
        val request = LaunchRequest(intent, source = LaunchSource.PRIMARY)

        proxy.launchActivity(request)

        argumentCaptor<Intent>().apply {
            verify(context).startActivity(capture())
            val capturedIntent = firstValue
            assertEquals(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY, capturedIntent.action)
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun testLaunchActivityWithActivityContextDoesNotAddFlag() {
        val activityContext: Activity = mock()
        val componentName = ComponentName("com.test", "com.test.Activity")
        val intent = Intent().setComponent(componentName)
        val request = LaunchRequest(intent, source = LaunchSource.PRIMARY)

        proxy.launchActivity(request, activityContext)

        argumentCaptor<Intent>().apply {
            verify(activityContext).startActivity(capture())
            val capturedIntent = firstValue
            assertEquals(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY, capturedIntent.action)
            assertEquals(0, capturedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
