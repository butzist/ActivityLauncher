package de.szalkowski.activitylauncher.entrypoint

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.data.launcher.ShortcutCreatorImpl
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
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
class ShortcutDatabaseFlowTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val recentsRepository: RecentsRepository = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val getActivityIconUseCase: GetActivityIconUseCase = mock()

    @BindValue
    val iconLoader: IconLoader = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val settingsRepository: SettingsRepository = mock()

    @BindValue
    val backupRepository: BackupRepository = mock()

    @BindValue
    val favoritesRepository: FavoritesRepository = mock()

    private lateinit var icon: androidx.core.graphics.drawable.IconCompat

    @Before
    fun init() {
        hiltRule.inject()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        icon = androidx.core.graphics.drawable.IconCompat.createWithResource(context, android.R.mipmap.sym_def_app_icon)
        whenever(getActivityIconUseCase(anyOrNull(), any())).thenReturn(icon)
        whenever(packageRepository.getActivity(any())).thenAnswer { invocation ->
            val component = invocation.getArgument<ComponentName>(0)
            MyActivityInfo(component, "Test Activity", null, false)
        }
    }

    @Test
    fun testShortcutCreationRecordsToDatabase_NoDuplicates() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val creator = ShortcutCreatorImpl(context, intentSigner, shortcutsRepository)

            val componentName = ComponentName("com.test", "com.test.Activity")
            val request = ShortcutRequest("Test App", Intent().setComponent(componentName), icon)

            // Record first time
            creator.createLauncherIcon(request, 1L)

            // Verify that it DID NOT call recordShortcut because ID was provided
            verify(shortcutsRepository, never()).recordShortcut(any())

            // Record second time without ID
            whenever(shortcutsRepository.recordShortcut(any())).thenReturn(2L)
            creator.createLauncherIcon(request, null)

            verify(shortcutsRepository, timeout(1000).times(1)).recordShortcut(eq(request))
        }
    }

    @Test
    fun testShortcutLaunchRecordsToRecents() {
        val launchUseCase = LaunchActivityUseCase(activityLauncher, activityLauncherProxy, recentsRepository, packageRepository, getActivityIconUseCase)

        val componentName = ComponentName("com.test", "com.test.Activity")
        val request = LaunchRequest(Intent().setComponent(componentName))

        launchUseCase.invoke(request)

        // Verify it was recorded to the recents database
        val captor = argumentCaptor<ShortcutRequest>()
        verify(recentsRepository).addActivity(captor.capture())
        assertEquals("Test Activity", captor.firstValue.name)
        assertEquals(componentName, captor.firstValue.intent.component)
    }
}
