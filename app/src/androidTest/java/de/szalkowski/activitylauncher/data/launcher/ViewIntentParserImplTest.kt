package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ViewIntentParserImplTest {
    private lateinit var parser: ViewIntentParserImpl

    @Before
    fun setup() {
        parser = ViewIntentParserImpl()
    }

    @Test
    fun testParseValidDeepLink() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://activitylauncher.net/activity/com.example/.MainActivity")
        }

        val request = parser.parseLaunchRequest(intent)

        assertEquals("com.example", request?.intent?.component?.packageName)
        assertEquals("com.example.MainActivity", request?.intent?.component?.className)
        assertEquals(LaunchSource.DEEPLINK, request?.source)
    }

    @Test
    fun testParseShortcutIntentInComponentName() {
        val launchIntent = Intent().apply {
            component = ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
        }

        val request = parser.parseLaunchRequest(intent)
        assertEquals("com.test", request?.intent?.component?.packageName)
        assertEquals("com.test.Activity", request?.intent?.component?.className)
        assertEquals(LaunchSource.PRIMARY, request?.source)
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
        val launchIntentUri = "intent://#Intent;component=com.test/.Activity;end"
        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            android.R.drawable.ic_menu_add,
        )
        val intent = Intent().apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, "Test Name")
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntentUri)
            putExtra(ShortcutCreator.INTENT_EXTRA_ICON, icon.toBundle())
        }

        val request = parser.parseShortcutRequest(intent)
        assertEquals("Test Name", request?.name)
        assertEquals("com.test", request?.intent?.component?.packageName)
        assertEquals("com.test.Activity", request?.intent?.component?.className)
        assertEquals(LaunchSource.PRIMARY, request?.source)
    }

    @Test
    fun testParseLaunchSource_SavedShortcut() {
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ID, "uuid-123")
        }

        val source = parser.parseLaunchSource(intent)
        assertNotNull(source)
        assertEquals("uuid-123", source?.id)
    }

    @Test
    fun testParseLaunchSource_SignedShortcut() {
        val launchIntent = Intent().apply {
            component = ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, "valid_sign")
        }

        val source = parser.parseLaunchSource(intent)
        assertNotNull(source)
        assertEquals("com.test", source?.intent?.component?.packageName)
        assertEquals("valid_sign", source?.signature)
    }

    @Test
    fun testParseLaunchSource_Priority() {
        val launchIntent = Intent().apply {
            component = ComponentName("com.test", "com.test.Activity")
        }
        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_SHORTCUT_ID, "uuid-123")
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, "valid_sign")
        }

        val source = parser.parseLaunchSource(intent)
        assertNotNull(source)
        assertEquals("uuid-123", source?.id)
        assertEquals("valid_sign", source?.signature)
    }
}
