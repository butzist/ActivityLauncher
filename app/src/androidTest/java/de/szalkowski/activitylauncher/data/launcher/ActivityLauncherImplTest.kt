package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ActivityLauncherImplTest {
    private lateinit var context: Context
    private lateinit var activityLauncher: ActivityLauncher
    private val spyContext = spy(ApplicationProvider.getApplicationContext<Context>())

    @Before
    fun setup() {
        context = spyContext
        activityLauncher = ActivityLauncherImpl(context)
    }

    @Test
    fun testLaunchActivityWithExtras() {
        val componentName = ComponentName("com.android.settings", "com.android.settings.Settings")
        val extras = Bundle().apply {
            putString("test_key", "test_value")
        }

        // We can't easily verify startActivity on a real context without mocking it,
        // which we are doing with spyContext.
        doNothing().whenever(spyContext).startActivity(any())

        activityLauncher.launchActivity(componentName, extras)

        argumentCaptor<Intent>().apply {
            verify(spyContext).startActivity(capture())
            val intent = firstValue
            assertEquals(componentName, intent.component)
            assertEquals("test_value", intent.getStringExtra("test_key"))
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
