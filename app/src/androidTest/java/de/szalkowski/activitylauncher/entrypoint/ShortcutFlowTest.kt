package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher
import de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ShortcutFlowTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    val viewIntentParser: de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser = mock()

    @BindValue
    val settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @Before
    fun init() {
        hiltRule.inject()

        // Stub viewIntentParser to handle the URI parsing in ShortcutActivity
        whenever(viewIntentParser.parseShortcutIntent(any())).thenAnswer { invocation ->
            val uri = invocation.getArgument<String>(0)
            try {
                Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            } catch (_: Exception) {
                try {
                    Intent.parseUri(uri, 0)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    @Test
    fun testStage3_LaunchShortcutFlow() {
        // Stage 3: Receive LAUNCH intent and start activity
        val componentName = ComponentName("com.test", "com.test.Activity")
        val extras = android.os.Bundle().apply { putString("key", "value") }
        val launchIntent = Intent().apply {
            component = componentName
            putExtras(extras)
        }
        val signature = "valid_signature"

        whenever(intentSigner.validateIntentSignature(any<Intent>(), eq(signature), isNull())).thenReturn(true)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher).launchActivity(eq(componentName), argThat { this?.getString("key") == "value" })
        }
    }

    @Test
    fun testStage3_LaunchShortcutFlowInvalidSignature() {
        // Stage 3: Reject if signature mismatch
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }
        val signature = "invalid_signature"

        whenever(intentSigner.validateIntentSignature(any<Intent>(), eq(signature), isNull())).thenReturn(false)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher, never()).launchActivity(any(), anyOrNull())
        }
    }

    @Test
    fun testStage2_CreateShortcutFlowDoesNotCrash() {
        // Stage 2: Receive CREATE intent
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }
        val icon = androidx.core.graphics.drawable.IconCompat.createWithBitmap(android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888))

        val intent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, "Test App")
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_ICON, icon.toBundle())
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use { scenario ->
            // On API 26+, there is a delay before finishing.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Thread.sleep(1000)
            }
            // Success if it doesn't crash and reaches destroyed state (finishes)
            assert(scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED)
            verify(shortcutCreator).createLauncherIcon(any<String>(), any<ComponentName>(), any<androidx.core.graphics.drawable.IconCompat>(), anyOrNull<android.os.Bundle>(), any<Boolean>())
        }
    }

    @Test
    fun testLaunchActivityFlow() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }

        val intent = Intent(ActivityLauncherProxy.INTENT_LAUNCH_ACTIVITY).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher).launchActivity(eq(componentName), anyOrNull<android.os.Bundle>())
        }
    }

    @Test
    fun testLaunchShortcutDelegation() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }
        val signature = "valid_signature"
        val launchPlugin = "com.plugin/.LaunchActivity"

        whenever(intentSigner.validateIntentSignature(any<Intent>(), eq(signature), eq(launchPlugin))).thenReturn(true)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, launchPlugin)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher, never()).launchActivity(any<ComponentName>(), anyOrNull<android.os.Bundle>())
            // It should start the plugin activity instead.
            // We can't easily verify startActivity from here without mocking Context,
            // but ShortcutActivity is using the real Context.
        }
    }
}
