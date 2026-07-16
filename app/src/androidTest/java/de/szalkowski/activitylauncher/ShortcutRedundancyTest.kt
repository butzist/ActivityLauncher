package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.entrypoint.ShortcutActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ShortcutRedundancyTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val activityLauncher: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: de.szalkowski.activitylauncher.domain.launcher.ActivityLauncherProxy = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @BindValue
    val settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @BindValue
    val backupRepository: de.szalkowski.activitylauncher.domain.settings.BackupRepository = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @Before
    fun init() {
        hiltRule.inject()
        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        whenever(recentsRepository.getRecentsFlow()).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
    }

    @Test
    fun testShortcutCreation_UnwrapsAndRecordsCorrectComponent() = runBlocking {
        val targetComponent = ComponentName("com.test", "com.test.Activity")
        val targetIntent = Intent().apply { component = targetComponent }
        val icon = androidx.core.graphics.drawable.IconCompat.createWithResource(ApplicationProvider.getApplicationContext(), android.R.drawable.sym_def_app_icon)

        // wrapped intent as if created by Proxy
        val wrappedIntent = Intent(ShortcutCreator.INTENT_LAUNCH_SHORTCUT).apply {
            component = ComponentName(ApplicationProvider.getApplicationContext<Context>(), ShortcutActivity::class.java)
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, targetIntent.toUri(Intent.URI_INTENT_SCHEME))
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, "Test App")
        }

        val createIntent = Intent(ShortcutCreatorProxy.INTENT_CREATE_SHORTCUT).apply {
            putExtra(ShortcutCreator.INTENT_EXTRA_NAME, "Test App")
            putExtra(ShortcutCreator.INTENT_EXTRA_INTENT, wrappedIntent.toUri(Intent.URI_INTENT_SCHEME))
            setClassName(ApplicationProvider.getApplicationContext(), ShortcutActivity::class.java.name)
        }

        // Mock ViewIntentParser to UNWRAP the intent (simulating the fix in ViewIntentParserImpl)
        whenever(viewIntentParser.parseShortcutRequest(any())).thenReturn(
            ShortcutRequest("Test App", targetIntent, icon),
        )
        whenever(shortcutsRepository.recordShortcut(any())).thenReturn(456L)

        ActivityScenario.launch<ShortcutActivity>(createIntent).use {
            Thread.sleep(1000)

            // Verify that ShortcutsRepository was called with the TARGET component, not ShortcutActivity
            val recordCaptor = argumentCaptor<ShortcutRequest>()
            verify(shortcutsRepository).recordShortcut(recordCaptor.capture())
            assertEquals(targetComponent, recordCaptor.firstValue.intent.component)

            // Verify that ShortcutCreator was called with the ID from recording
            val creatorCaptor = argumentCaptor<ShortcutRequest>()
            verify(shortcutCreator).createLauncherIcon(creatorCaptor.capture(), eq(456L))
        }
    }
}
