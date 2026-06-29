package de.szalkowski.activitylauncher.presentation.common

import android.content.ComponentName
import androidx.lifecycle.SavedStateHandle
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PluginChooserViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PluginChooserViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PluginChooserViewModel(SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockComponentName(pkg: String, cls: String): ComponentName = mock {
        on { packageName } doReturn pkg
        on { className } doReturn cls
    }

    @Test
    fun `should complete selection immediately if only one plugin exists for LAUNCH`() = runTest {
        val launchPlugins = listOf(PluginInfo("P1", createMockComponentName("pkg", "cls"), null))
        val shortcutPlugins = listOf(PluginInfo("P2", createMockComponentName("pkg2", "cls2"), null))

        viewModel.setPlugins(PluginChooserDialogFragment.PluginAction.LAUNCH, launchPlugins, shortcutPlugins)

        assertTrue(viewModel.isSelectionComplete.first())
    }

    @Test
    fun `should NOT complete selection if multiple launch plugins exist for LAUNCH`() = runTest {
        val launchPlugins = listOf(
            PluginInfo("P1", createMockComponentName("pkg1", "cls1"), null),
            PluginInfo("P2", createMockComponentName("pkg2", "cls2"), null),
        )
        viewModel.setPlugins(PluginChooserDialogFragment.PluginAction.LAUNCH, launchPlugins, emptyList())

        assertFalse(viewModel.isSelectionComplete.first())

        viewModel.selectLaunchPlugin(launchPlugins[0])
        assertTrue(viewModel.isSelectionComplete.first())
    }

    @Test
    fun `should NOT complete selection for SHORTCUT until shortcut plugin is selected`() = runTest {
        val launchPlugins = listOf(PluginInfo("P1", createMockComponentName("pkg1", "cls1"), null))
        val shortcutPlugins = listOf(
            PluginInfo("S1", createMockComponentName("spkg1", "scls1"), null),
            PluginInfo("S2", createMockComponentName("spkg2", "scls2"), null),
        )

        viewModel.setPlugins(PluginChooserDialogFragment.PluginAction.SHORTCUT, launchPlugins, shortcutPlugins)

        // Even if only one launch plugin, SHORTCUT requires shortcut plugin selection if multiple exist
        assertFalse(viewModel.isSelectionComplete.first())

        viewModel.selectShortcutPlugin(shortcutPlugins[0])
        assertTrue(viewModel.isSelectionComplete.first())
    }

    @Test
    fun `should require launch plugin selection for SHORTCUT if multiple exist`() = runTest {
        val launchPlugins = listOf(
            PluginInfo("P1", createMockComponentName("pkg1", "cls1"), null),
            PluginInfo("P2", createMockComponentName("pkg2", "cls2"), null),
        )
        val shortcutPlugins = listOf(PluginInfo("S1", createMockComponentName("spkg1", "scls1"), null))

        viewModel.setPlugins(PluginChooserDialogFragment.PluginAction.SHORTCUT, launchPlugins, shortcutPlugins)

        assertFalse(viewModel.isSelectionComplete.first())

        viewModel.selectLaunchPlugin(launchPlugins[0])
        assertTrue(viewModel.isSelectionComplete.first())
    }

    @Test
    fun `getResult should return selected component names`() {
        val p1 = createMockComponentName("pkg1", "cls1")
        val s1 = createMockComponentName("spkg1", "scls1")
        viewModel.selectLaunchPlugin(PluginInfo("P1", p1, null))
        viewModel.selectShortcutPlugin(PluginInfo("S1", s1, null))

        val result = viewModel.getResult()
        assertEquals(p1, result.launchPlugin)
        assertEquals(s1, result.shortcutPlugin)
    }
}
