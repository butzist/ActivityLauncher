package de.szalkowski.activitylauncher.core.util

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawableUtilTest {

    @Test
    fun testGetLauncherLargeIconSize() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val size = context.getLauncherLargeIconSize()
        assertTrue("Icon size should be positive", size > 0)
    }

    @Test
    fun testBitmapResizeMaintainAspectRatio() {
        // Landscape
        val landscape = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val resizedLandscape = landscape.resize(100)
        assertEquals(100, resizedLandscape.width)
        assertEquals(50, resizedLandscape.height)

        // Portrait
        val portrait = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val resizedPortrait = portrait.resize(100)
        assertEquals(50, resizedPortrait.width)
        assertEquals(100, resizedPortrait.height)

        // Square
        val square = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val resizedSquare = square.resize(100)
        assertEquals(100, resizedSquare.width)
        assertEquals(100, resizedSquare.height)
    }

    @Test
    fun testBitmapResizeNoUpscale() {
        val small = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val resized = small.resize(100)
        assertEquals(50, resized.width)
        assertEquals(50, resized.height)
    }
}
