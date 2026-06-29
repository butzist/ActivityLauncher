package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ViewIntentParserImplTest {
    private lateinit var parser: ViewIntentParserImpl
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @Before
    fun setup() {
        parser = ViewIntentParserImpl(getActivityIconUseCase)
    }

    @Test
    fun testParseValidDeepLink() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://activitylauncher.net/activity/com.example/.MainActivity")
        }

        val request = parser.parseLaunchRequest(intent)

        assertEquals("com.example", request?.component?.packageName)
        assertEquals("com.example.MainActivity", request?.component?.className)
    }

    @Test
    fun testParseShortcutIntentInComponentName() {
        val launchIntent = Intent().apply {
            component = android.content.ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        }

        val request = parser.parseLaunchRequest(intent)
        assertEquals("com.test", request?.component?.packageName)
        assertEquals("com.test.Activity", request?.component?.className)
    }

    @Test
    fun testReturnNullForInvalidAction() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            data = Uri.parse("https://activitylauncher.net/activity/com.example/.MainActivity")
        }

        assertNull(parser.parseLaunchRequest(intent))
    }

    @Test
    fun testParseShortcutRequest() {
        val launchIntent = Intent().apply {
            component = android.content.ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, "Test Name")
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        }

        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(mock())

        val request = parser.parseShortcutRequest(intent)
        assertEquals("Test Name", request?.name)
        assertEquals("com.test", request?.component?.packageName)
        assertEquals("com.test.Activity", request?.component?.className)
    }
}
