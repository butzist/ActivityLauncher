package de.szalkowski.activitylauncher.data.database

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SerializationUtilsTest {

    @Test
    fun testIconSerializationResource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = IconCompat.createWithResource(context, android.R.drawable.ic_menu_save)

        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertEquals(IconCompat.TYPE_RESOURCE, restored?.type)
        assertEquals(context.packageName, restored?.resPackage)
        assertEquals(android.R.drawable.ic_menu_save, restored?.resId)
    }

    @Test
    fun testIconSerializationBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val icon = IconCompat.createWithBitmap(bitmap)
        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertEquals(IconCompat.TYPE_BITMAP, restored?.type)
    }

    @Test
    fun testIconSerializationAdaptiveBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val icon = IconCompat.createWithAdaptiveBitmap(bitmap)
        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertEquals(IconCompat.TYPE_ADAPTIVE_BITMAP, restored?.type)
    }

    @Test
    fun testIconSerializationUri() {
        val uri = Uri.parse("content://test/image.png")
        val icon = IconCompat.createWithContentUri(uri)
        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertEquals(IconCompat.TYPE_URI, restored?.type)
        assertEquals(uri.toString(), restored?.uri.toString())
    }

    @Test
    fun testIntentSerialization() {
        val intent = Intent("action.TEST")
        intent.putExtra("extra", "value")
        val uri = SerializationUtils.intentToUri(intent)
        val restored = SerializationUtils.uriToIntent(uri)
        assertEquals("action.TEST", restored.action)
        assertEquals("value", restored.getStringExtra("extra"))
    }
}
