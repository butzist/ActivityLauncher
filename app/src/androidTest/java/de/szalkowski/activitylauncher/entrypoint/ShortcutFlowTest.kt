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
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import org.junit.Assert.assertEquals
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

        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(packageRepository.packagesFlow).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(false))

        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val componentName = invocation.getArgument<ComponentName>(0)
            de.szalkowski.activitylauncher.domain.model.MyActivityInfo(
                componentName,
                "Test Activity",
                null,
                false,
            )
        }

        whenever(packageRepository.getActivities(any())).thenAnswer { invocation ->
            val packageName = invocation.getArgument<String>(0)
            de.szalkowski.activitylauncher.domain.model.PackageActivities(
                packageName,
                "Test App",
                null,
                emptyList(),
            )
        }

        // Stub viewIntentParser to handle the URI parsing in ShortcutActivity
        whenever(viewIntentParser.parseShortcutRequest(any())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock ->
            val intent = invocation.getArgument<Intent>(0)
            val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return@thenAnswer null
            val launchIntent = try {
                Intent.parseUri(launchIntentStr, Intent.URI_INTENT_SCHEME)
            } catch (_: Exception) {
                null
            } ?: return@thenAnswer null

            val appName = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_NAME) ?: ""
            val launchPluginStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN)
            val launchPlugin = launchPluginStr?.let { ComponentName.unflattenFromString(it) }

            ShortcutRequest(
                name = appName,
                component = launchIntent.component!!,
                icon = mock<androidx.core.graphics.drawable.IconCompat>(),
                extras = launchIntent.extras,
                launcherPlugin = launchPlugin,
            )
        }

        whenever(viewIntentParser.parseLaunchRequest(any())).thenAnswer { invocation: org.mockito.invocation.InvocationOnMock ->
            val intent = invocation.getArgument<Intent>(0)
            val launchIntentStr = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT) ?: return@thenAnswer null
            val launchIntent = try {
                Intent.parseUri(launchIntentStr, Intent.URI_INTENT_SCHEME)
            } catch (_: Exception) {
                null
            } ?: return@thenAnswer null

            LaunchRequest(
                component = launchIntent.component!!,
                extras = launchIntent.extras,
            )
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

        whenever(intentSigner.validateRequestSignature(any(), eq(signature))).thenReturn(true)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            val captor = argumentCaptor<LaunchRequest>()
            verify(activityLauncher).launchActivity(captor.capture())
            assertEquals("com.test", captor.firstValue.component.packageName)
            assertEquals("com.test.Activity", captor.firstValue.component.className)
            assertEquals("value", captor.firstValue.extras?.getString("key"))
        }
    }

    @Test
    fun testStage3_LaunchShortcutFlowInvalidSignatureRedirect() {
        // Stage 3: Redirect to MainActivity if signature mismatch
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }
        val signature = "invalid_signature"

        whenever(intentSigner.validateRequestSignature(any(), eq(signature))).thenReturn(false)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher, never()).launchActivity(any())
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
            verify(shortcutCreator).createLauncherIcon(any())
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
            val captor = argumentCaptor<LaunchRequest>()
            verify(activityLauncher).launchActivity(captor.capture())
            assertEquals(componentName, captor.firstValue.component)
        }
    }

    @Test
    fun testLaunchShortcutDelegation() {
        val componentName = ComponentName("com.test", "com.test.Activity")
        val launchIntent = Intent().apply { component = componentName }
        val signature = "valid_signature"
        val launchPlugin = "com.plugin/.LaunchActivity"

        whenever(intentSigner.validateRequestSignature(any(), eq(signature))).thenReturn(true)

        val intent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, launchIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_SIGNATURE, signature)
            putExtra(ShortcutCreator.INTENT_EXTRA_LAUNCH_PLUGIN, launchPlugin)
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        ActivityScenario.launch<ShortcutActivity>(intent).use {
            verify(activityLauncher, never()).launchActivity(any())
        }
    }
}
