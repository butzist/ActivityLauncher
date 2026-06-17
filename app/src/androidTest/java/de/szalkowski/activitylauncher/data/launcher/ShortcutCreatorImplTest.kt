package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ShortcutCreatorImplTest {
    private lateinit var context: Context
    private val intentSigner: IntentSigner = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private lateinit var shortcutCreator: ShortcutCreator
    private val spyContext = spy(ApplicationProvider.getApplicationContext<Context>())

    @Before
    fun setup() {
        context = spyContext
        shortcutCreator = ShortcutCreatorImpl(context, intentSigner, getActivityIconUseCase)

        val icon = androidx.core.graphics.drawable.IconCompat.createWithBitmap(
            android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888),
        )
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(icon)
        whenever(intentSigner.signIntent(any())).thenReturn("signature")
    }

    @Test
    fun testCreateLauncherIcon() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val activityInfo = MyActivityInfo(componentName, "Test App", null, false)
        val extras = Bundle().apply { putString("key", "value") }

        if (Build.VERSION.SDK_INT >= 26) {
            // On API 26+, it uses ShortcutManager. We might not be able to verify it easily
            // without a lot of mocking of system services, but we can at least ensure it runs.
            shortcutCreator.createLauncherIcon(activityInfo, null, extras)
        } else {
            // On older APIs it sends a broadcast.
            shortcutCreator.createLauncherIcon(activityInfo, null, extras)

            argumentCaptor<Intent>().apply {
                verify(spyContext).sendBroadcast(capture())
                val intent = firstValue
                assertEquals("com.android.launcher.action.INSTALL_SHORTCUT", intent.action)
                val launchIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)
                assertEquals(componentName, launchIntent?.component)
                assertEquals("value", launchIntent?.getStringExtra("key"))
            }
        }
    }
}
