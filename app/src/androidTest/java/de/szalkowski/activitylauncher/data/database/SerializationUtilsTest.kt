package de.szalkowski.activitylauncher.data.database

import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SerializationUtilsTest {

    @Test
    fun testIconSerializationResource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = ActivityIcon.Resource(context.packageName, android.R.drawable.ic_menu_save, "android:drawable/ic_menu_save")

        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertTrue(restored is ActivityIcon.Resource)
        val res = restored as ActivityIcon.Resource
        assertEquals(context.packageName, res.packageName)
        assertEquals(android.R.drawable.ic_menu_save, res.resId)
        assertEquals("android:drawable/ic_menu_save", res.resourceName)
    }

    @Test
    fun testIconSerializationResourceNoId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = ActivityIcon.Resource(context.packageName, 0, "android:drawable/ic_menu_save")

        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertTrue(restored is ActivityIcon.Resource)
        val res = restored as ActivityIcon.Resource
        assertEquals(context.packageName, res.packageName)
        assertEquals(0, res.resId)
        assertEquals("android:drawable/ic_menu_save", res.resourceName)
    }

    @Test
    fun testIconSerializationBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val icon = ActivityIcon.BitmapIcon(bitmap, false)
        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertTrue(restored is ActivityIcon.BitmapIcon)
        val bmpIcon = restored as ActivityIcon.BitmapIcon
        assertEquals(false, bmpIcon.isAdaptive)
        assertEquals(10, bmpIcon.bitmap.width)
    }

    @Test
    fun testIconSerializationAdaptiveBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val icon = ActivityIcon.BitmapIcon(bitmap, true)
        val bytes = SerializationUtils.iconToByteArray(icon)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertTrue(restored is ActivityIcon.BitmapIcon)
        val bmpIcon = restored as ActivityIcon.BitmapIcon
        assertEquals(true, bmpIcon.isAdaptive)
    }

    @Test
    fun testIconSerializationLegacy() {
        val icon = IconCompat.createWithResource(InstrumentationRegistry.getInstrumentation().targetContext, android.R.drawable.ic_menu_add)
        val legacy = ActivityIcon.Legacy(icon.toBundle())
        val bytes = SerializationUtils.iconToByteArray(legacy)
        val restored = SerializationUtils.byteArrayToIcon(bytes)
        assertNotNull(restored)
        assertTrue(restored is ActivityIcon.Legacy)
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
