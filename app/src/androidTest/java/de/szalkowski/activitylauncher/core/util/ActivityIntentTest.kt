package de.szalkowski.activitylauncher.core.util

import android.content.ComponentName
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityIntentTest {

    @Test
    fun testGetActivityIntentFromIntentDef() {
        val componentName = ComponentName("pkg", "cls")
        val intentDef = IntentDef(
            action = Intent.ACTION_VIEW,
            data = "https://example.com",
            mimeType = "text/plain",
            categories = listOf(Intent.CATEGORY_BROWSABLE),
            extras = listOf(
                ExtraDef("key_string", "value", ExtraType.STRING),
                ExtraDef("key_int", "123", ExtraType.INT),
                ExtraDef("key_long", "456", ExtraType.LONG),
                ExtraDef("key_float", "1.2", ExtraType.FLOAT),
                ExtraDef("key_double", "3.4", ExtraType.DOUBLE),
                ExtraDef("key_bool", "true", ExtraType.BOOLEAN),
            ),
        )

        val intent = getActivityIntentFromIntentDef(componentName, intentDef)

        assertEquals(componentName, intent.component)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://example.com", intent.dataString)
        assertEquals("text/plain", intent.type)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertEquals("value", intent.getStringExtra("key_string"))
        assertEquals(123, intent.getIntExtra("key_int", 0))
        assertEquals(456L, intent.getLongExtra("key_long", 0L))
        assertEquals(1.2f, intent.getFloatExtra("key_float", 0f))
        assertEquals(3.4, intent.getDoubleExtra("key_double", 0.0), 0.001)
        assertTrue(intent.getBooleanExtra("key_bool", false))
    }

    @Test
    fun testGetIntentDefFromActivityIntent() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            setDataAndType(android.net.Uri.parse("mailto:test@example.com"), "message/rfc822")
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra("key_string", "value")
            putExtra("key_int", 123)
            putExtra("key_bool", true)
        }

        val intentDef = getIntentDefFromActivityIntent(intent)

        assertEquals(Intent.ACTION_SEND, intentDef.action)
        assertEquals("mailto:test@example.com", intentDef.data)
        assertEquals("message/rfc822", intentDef.mimeType)
        assertTrue(intentDef.categories.contains(Intent.CATEGORY_DEFAULT))

        val extras = intentDef.extras.associateBy { it.key }
        assertEquals("value", extras["key_string"]?.value)
        assertEquals(ExtraType.STRING, extras["key_string"]?.type)

        assertEquals("123", extras["key_int"]?.value)
        assertEquals(ExtraType.INT, extras["key_int"]?.type)

        assertEquals("true", extras["key_bool"]?.value)
        assertEquals(ExtraType.BOOLEAN, extras["key_bool"]?.type)
    }
}
