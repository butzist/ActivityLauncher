package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import org.junit.Assert.*
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
        assertTrue("Icon should be BitmapIcon", icon is ActivityIcon.BitmapIcon)
        val bitmapIcon = icon as ActivityIcon.BitmapIcon
        assertTrue("Bitmap should be resized", bitmapIcon.bitmap.width <= 256 && bitmapIcon.bitmap.height <= 256)

        // Cleanup
        tempFile.delete()
    }

    @Test
    fun testGetIconFromInvalidUri() {
        val invalidUri = Uri.parse("file:///non/existent/path.png")
        val result = iconLoader.getIcon(invalidUri)
        assertTrue("Result should be failure", result.isFailure)
    }

    @Test
    fun testGetIconOwnPackageUsesResource() {
        val componentName = ComponentName(context.packageName, "de.szalkowski.activitylauncher.entrypoint.MainActivity")
        val icon = iconLoader.getIcon(componentName)
        assertTrue("Icons from own package should use Resource", icon is ActivityIcon.Resource)
    }

    @Test
    fun testGetIconOtherPackageUsesResource() {
        // Use android settings as a guaranteed external package
        val componentName = ComponentName("com.android.settings", "com.android.settings.Settings")
        val icon = iconLoader.getIcon(componentName)

        assertTrue("Icons from other packages should use Resource", icon is ActivityIcon.Resource)
    }

    @Test
    fun testGetPackageIconOtherPackageUsesResource() {
        val icon = iconLoader.getPackageIcon("com.android.settings")
        assertTrue("Package icons from other apps should use Resource", icon is ActivityIcon.Resource)
    }

    @Test
    fun testTryGetIconOtherPackageUsesResource() {
        // A common resource in android framework
        val resStr = "android:drawable/ic_menu_add"
        val result = iconLoader.tryGetIcon(resStr)
        assertTrue(result.isSuccess)
        assertTrue("Icons from other packages (via tryGetIcon) should use Resource", result.getOrThrow() is ActivityIcon.Resource)
    }
}
