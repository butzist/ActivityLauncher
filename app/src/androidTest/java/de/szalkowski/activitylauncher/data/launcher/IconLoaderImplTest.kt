package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class IconLoaderImplTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var iconLoader: IconLoader

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetIconFromUri() {
        // Create a temporary large image
        val tempFile = File(context.cacheDir, "test_image.png")
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = Uri.fromFile(tempFile)

        // Load it via IconLoader
        val result = iconLoader.getIcon(uri)

        assertTrue("Result should be success", result.isSuccess)
        val icon = result.getOrNull()
        assertNotNull("Icon should not be null", icon)

        // Cleanup
        tempFile.delete()
    }

    @Test
    fun testGetIconFromInvalidUri() {
        val invalidUri = Uri.parse("file:///non/existent/path.png")
        val result = iconLoader.getIcon(invalidUri)
        assertTrue("Result should be failure", result.isFailure)
    }
}
