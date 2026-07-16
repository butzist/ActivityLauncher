package de.szalkowski.activitylauncher.domain.model

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import androidx.core.graphics.drawable.IconCompat
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class ActivityIconTest {

    @Test
    fun `from IconCompat works for resource type`() {
        val iconCompat = mock<IconCompat>()
        val bundle = mock<Bundle>()
        whenever(bundle.getInt("type")).thenReturn(IconCompat.TYPE_RESOURCE)
        whenever(bundle.getString("obj")).thenReturn("com.test")
        whenever(bundle.getInt("int1")).thenReturn(123)
        whenever(iconCompat.toBundle()).thenReturn(bundle)

        val activityIcon = ActivityIcon.from(iconCompat)

        assertTrue(activityIcon is ActivityIcon.Resource)
        val resource = activityIcon as ActivityIcon.Resource
        assertEquals("com.test", resource.packageName)
        assertEquals(123, resource.resId)
    }

    @Test
    fun `from IconCompat works for bitmap type`() {
        val iconCompat = mock<IconCompat>()
        val bitmap = mock<Bitmap>()
        val bundle = mock<Bundle>()
        whenever(bundle.getInt("type")).thenReturn(IconCompat.TYPE_BITMAP)
        whenever(bundle.getParcelable<Bitmap>("obj")).thenReturn(bitmap)
        whenever(iconCompat.toBundle()).thenReturn(bundle)

        val activityIcon = ActivityIcon.from(iconCompat)

        assertTrue(activityIcon is ActivityIcon.BitmapIcon)
        val bitmapIcon = activityIcon as ActivityIcon.BitmapIcon
        assertFalse(bitmapIcon.isAdaptive)
        assertEquals(bitmap, bitmapIcon.bitmap)
    }

    @Test
    fun `toShortcutIcon converts hardware bitmap and resizes`() {
        val context: Context = mock()
        val bitmap = mock<Bitmap>()
        whenever(bitmap.config).thenReturn(Bitmap.Config.RGB_565)
        whenever(bitmap.width).thenReturn(100)
        whenever(bitmap.height).thenReturn(100)

        val activityIcon = ActivityIcon.BitmapIcon(bitmap, false)
        val shortcutIcon = activityIcon.toShortcutIcon(context)

        assertNotNull(shortcutIcon)
        assertEquals(IconCompat.TYPE_BITMAP, shortcutIcon.type)
    }

    @Test
    fun `Parcelable implementation works correctly for Resource`() {
        val icon = ActivityIcon.Resource("com.test", 123)
        val parcel = mock<Parcel>()

        icon.writeToParcel(parcel, 0)
        verify(parcel).writeInt(0) // Type Resource
        verify(parcel).writeString("com.test")
        verify(parcel).writeInt(123)

        whenever(parcel.readInt()).thenReturn(0, 123)
        whenever(parcel.readString()).thenReturn("com.test", null)

        val restored = ActivityIcon.CREATOR.createFromParcel(parcel)
        assertEquals(icon, restored)
    }

    @Test
    fun `Parcelable implementation works correctly for BitmapIcon`() {
        val bitmap = mock<Bitmap>()
        whenever(bitmap.config).thenReturn(Bitmap.Config.ARGB_8888)
        val icon = ActivityIcon.BitmapIcon(bitmap, true)
        val parcel = mock<Parcel>()

        icon.writeToParcel(parcel, 0)
        // verify type write
        verify(parcel, atLeastOnce()).writeInt(1)
        verify(parcel).writeParcelable(any(), any())

        whenever(parcel.readInt()).thenReturn(1, 1) // Type BitmapIcon and isAdaptive
        whenever(parcel.readParcelable<Bitmap>(any())).thenReturn(bitmap)

        val restored = ActivityIcon.CREATOR.createFromParcel(parcel)
        assertTrue(restored is ActivityIcon.BitmapIcon)
        val restoredBitmapIcon = restored as ActivityIcon.BitmapIcon
        assertTrue(restoredBitmapIcon.isAdaptive)
        assertEquals(bitmap, restoredBitmapIcon.bitmap)
    }

    @Test
    fun `factory methods create correct instances`() {
        val resIcon = ActivityIcon.fromResource("pkg", 1)
        assertTrue(resIcon is ActivityIcon.Resource)
        assertEquals("pkg", (resIcon as ActivityIcon.Resource).packageName)

        val bitmap = mock<Bitmap>()
        val bmpIcon = ActivityIcon.fromBitmap(bitmap, true)
        assertTrue(bmpIcon is ActivityIcon.BitmapIcon)
        assertTrue((bmpIcon as ActivityIcon.BitmapIcon).isAdaptive)
    }
}
