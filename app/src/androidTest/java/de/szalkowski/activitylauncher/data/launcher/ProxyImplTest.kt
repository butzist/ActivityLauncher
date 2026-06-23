package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ProxyImplTest {
    private lateinit var context: Context
    private val spyContext = spy(ApplicationProvider.getApplicationContext<Context>())
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val intentSigner: IntentSigner = mock()

    private lateinit var activityLauncherProxy: ActivityLauncherProxy
    private lateinit var shortcutCreatorProxy: ShortcutCreatorProxy

    @Before
    fun setup() {
        context = spyContext
        activityLauncherProxy = ActivityLauncherProxyImpl(context)
        shortcutCreatorProxy = ShortcutCreatorProxyImpl(context, getActivityIconUseCase, intentSigner)

        val icon = androidx.core.graphics.drawable.IconCompat.createWithBitmap(
            android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888),
        )
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(icon)

        doNothing().whenever(spyContext).startActivity(any())
    }

    @Test
    fun testActivityLauncherProxySendsIntent() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val extras = Bundle().apply { putString("a", "b") }

        activityLauncherProxy.launchActivity(componentName, extras)

        argumentCaptor<Intent>().apply {
            verify(spyContext).startActivity(capture())
            val intent = firstValue
            assertEquals(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY, intent.action)
            val launchIntentUri = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
            val launchIntent = Intent.parseUri(launchIntentUri, Intent.URI_INTENT_SCHEME)
            assertEquals(componentName, launchIntent.component)
            assertEquals("b", launchIntent.getStringExtra("a"))
        }
    }

    @Test
    fun testShortcutCreatorProxySendsIntent() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val activityInfo = MyActivityInfo(componentName, "Test", null, false)

        shortcutCreatorProxy.createLauncherIcon(activityInfo)

        argumentCaptor<Intent>().apply {
            verify(spyContext).startActivity(capture())
            val intent = firstValue
            assertEquals(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT, intent.action)
            assertEquals("Test", intent.getStringExtra("extra_name"))
        }
    }
}
