package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import de.szalkowski.activitylauncher.app.di.CoreServicesModule
import de.szalkowski.activitylauncher.app.di.FeatureServicesModule
import de.szalkowski.activitylauncher.domain.external.SupportReminder
import de.szalkowski.activitylauncher.domain.launcher.*
import de.szalkowski.activitylauncher.domain.model.*
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.CalculateSupportReminderUseCase
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import de.szalkowski.activitylauncher.presentation.activities.ActivityDetailsFragment
import de.szalkowski.activitylauncher.presentation.activities.DetailsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@HiltAndroidTest
@UninstallModules(CoreServicesModule::class, FeatureServicesModule::class)
@RunWith(AndroidJUnit4::class)
class ShortcutsIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @BindValue
    val shortcutsRepository: ShortcutsRepository = mock()

    @BindValue
    val settingsRepository: de.szalkowski.activitylauncher.domain.settings.SettingsRepository = mock()

    @BindValue
    val favoritesRepository: de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository = mock()

    @BindValue
    val recentsRepository: de.szalkowski.activitylauncher.domain.recents.RecentsRepository = mock()

    @BindValue
    val packageRepository: de.szalkowski.activitylauncher.domain.packages.PackageRepository = mock()

    @BindValue
    val adManager: de.szalkowski.activitylauncher.domain.external.AdManager = mock()

    @BindValue
    val analyticsLogger: de.szalkowski.activitylauncher.domain.external.AnalyticsLogger = mock()

    @BindValue
    val getActivityIconUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase = mock()

    @BindValue
    val supportReminder: SupportReminder = mock()

    @BindValue
    val shortcutCreator: ShortcutCreator = mock()

    @BindValue
    val shortcutCreatorProxy: ShortcutCreatorProxy = mock()

    @BindValue
    val viewIntentParser: ViewIntentParser = mock()

    @BindValue
    val activityLauncher: ActivityLauncher = mock()

    @BindValue
    val activityLauncherProxy: ActivityLauncherProxy = mock()

    @BindValue
    val intentSigner: IntentSigner = mock()

    @BindValue
    val iconLoader: de.szalkowski.activitylauncher.domain.launcher.IconLoader = mock()

    @BindValue
    val backupRepository: de.szalkowski.activitylauncher.domain.settings.BackupRepository = mock()

    @BindValue
    val activitySharer: de.szalkowski.activitylauncher.domain.external.ActivitySharer = mock()

    @BindValue
    lateinit var calculateSupportReminderUseCase: CalculateSupportReminderUseCase

    private val shortcutsFlow = MutableStateFlow<List<ShortcutsRepository.ManagedShortcut>>(emptyList())

    @Before
    fun init() {
        calculateSupportReminderUseCase = CalculateSupportReminderUseCase(supportReminder)

        whenever(settingsRepository.disclaimerAccepted).thenReturn(true)
        whenever(supportReminder.shouldDisplayReminder()).thenReturn(false)
        whenever(favoritesRepository.getFavorites()).thenReturn(emptySet())
        whenever(recentsRepository.getRecentActivities()).thenReturn(emptyList())
        whenever(favoritesRepository.getFavoritesFlow()).thenReturn(MutableStateFlow(emptyList()))
        whenever(recentsRepository.getRecentsFlow()).thenReturn(MutableStateFlow(emptyList()))

        whenever(packageRepository.packagesFlow).thenReturn(MutableStateFlow(emptyList()))
        whenever(packageRepository.isSyncing).thenReturn(MutableStateFlow(value = false))
        whenever(packageRepository.isLoaded).thenReturn(true)

        whenever(shortcutsRepository.getShortcutsFlow()).thenReturn(shortcutsFlow)

        val icon = ActivityIcon.Resource(ApplicationProvider.getApplicationContext<android.content.Context>().packageName, android.R.drawable.sym_def_app_icon)
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(icon)

        whenever(shortcutCreatorProxy.hasMultipleHandlers()).thenReturn(false)

        runBlocking {
            whenever(shortcutsRepository.recordShortcut(any(), anyOrNull())).thenAnswer { invocation ->
                val request = invocation.getArgument<ShortcutRequest>(0)
                if (request.name == "Initial Name") "id-1" else "id-new"
            }
        }

        hiltRule.inject()
    }

    @Test
    fun testManagedShortcutUpdateAndPinning() {
        val componentName = ComponentName("com.test", "Activity")
        val icon = ActivityIcon.Resource(ApplicationProvider.getApplicationContext<android.content.Context>().packageName, android.R.drawable.sym_def_app_icon)
        val initialRequest = ShortcutRequest("Initial Name", Intent().setComponent(componentName), icon, source = LaunchSource.SAVED)
        val initialShortcut = ShortcutsRepository.ManagedShortcut("id-1", initialRequest, 1000L)

        shortcutsFlow.value = listOf(initialShortcut)

        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.ShortcutsFragment)
            }

            Thread.sleep(1000)

            val updatedRequest = initialRequest.copy(name = "Updated Name")
            val result = DetailsResult.Save(updatedRequest, "id-1")

            scenario.onActivity { activity ->
                val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)!!
                val fragment = navHostFragment.childFragmentManager.fragments.first()
                fragment.setFragmentResult(ActivityDetailsFragment.RESULT_SAVED, bundleOf(ActivityDetailsFragment.EXTRA_SAVED_SHORTCUT to result))
            }

            runBlocking {
                verify(shortcutsRepository, timeout(5000)).updateShortcut(eq("id-1"), eq(updatedRequest))
            }

            clearInvocations(shortcutsRepository, shortcutCreator)

            val updatedShortcut = ShortcutsRepository.ManagedShortcut("id-1", updatedRequest, 2000L)
            shortcutsFlow.value = listOf(updatedShortcut)

            Thread.sleep(1000)

            scenario.onActivity { activity ->
                val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvShortcuts)
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(0)
                viewHolder?.itemView?.performClick()
            }

            Thread.sleep(2000)

            runBlocking {
                verify(shortcutsRepository).updateShortcut(eq("id-1"), any())
                verify(shortcutsRepository, never()).recordShortcut(any(), anyOrNull())

                val captor = argumentCaptor<ShortcutProxyRequest>()
                verify(shortcutCreator).createLauncherIcon(captor.capture(), eq("id-1"), anyOrNull())
                assertEquals("Updated Name", captor.firstValue.name)
            }
        }
    }
}
