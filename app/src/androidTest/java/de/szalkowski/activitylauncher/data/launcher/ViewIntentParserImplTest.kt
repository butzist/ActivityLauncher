package de.szalkowski.activitylauncher.data.launcher

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

        val component = parser.componentNameFromIntent(intent)
        val packageName = parser.packageFromIntent(intent)

        assertEquals("com.example", component?.packageName)
        assertEquals("com.example.MainActivity", component?.className)
        assertEquals("com.example", packageName)
    }

    @Test
    fun testReturnNullForInvalidAction() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            data = Uri.parse("https://activitylauncher.net/activity/com.example/.MainActivity")
        }

        assertNull(parser.componentNameFromIntent(intent))
    }

    @Test
    fun testParseShortcutIntentLegacy() {
        val originalIntent = Intent().apply {
            component = android.content.ComponentName("com.test", "com.test.Activity")
            putExtra("key", "value")
        }
        val uri = originalIntent.toUri(0)

        val parsedIntent = parser.parseShortcutIntent(uri)
        assertEquals("com.test", parsedIntent?.component?.packageName)
        assertEquals("value", parsedIntent?.getStringExtra("key"))
    }

    @Test
    fun testParseShortcutIntentModern() {
        val originalIntent = Intent().apply {
            component = android.content.ComponentName("com.test", "com.test.Activity")
            putExtra("key", "value")
        }
        val uri = originalIntent.toUri(Intent.URI_INTENT_SCHEME)

        val parsedIntent = parser.parseShortcutIntent(uri)
        assertEquals("com.test", parsedIntent?.component?.packageName)
        assertEquals("value", parsedIntent?.getStringExtra("key"))
    }
}
