package de.szalkowski.activitylauncher.presentation.intent

import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditIntentViewModelTest {
    private lateinit var viewModel: EditIntentViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditIntentViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateAction updates intentDef`() {
        viewModel.updateAction("new_action")
        assertEquals("new_action", viewModel.intentDef.value.action)
    }

    @Test
    fun `clear resets intentDef`() {
        viewModel.init(IntentDef(action = "something", categories = listOf("cat")))
        viewModel.clear()
        assertEquals(IntentDef(), viewModel.intentDef.value)
    }

    @Test
    fun `addExtra adds a new extra`() {
        viewModel.addExtra()
        assertEquals(1, viewModel.intentDef.value.extras.size)
        assertEquals(ExtraType.STRING, viewModel.intentDef.value.extras[0].type)
    }

    @Test
    fun `updateExtra updates specific extra`() {
        viewModel.addExtra()
        val newExtra = ExtraDef("key", "value", ExtraType.INT)
        viewModel.updateExtra(0, newExtra)
        assertEquals(newExtra, viewModel.intentDef.value.extras[0])
    }

    @Test
    fun `removeCategory removes specific category`() {
        val initial = IntentDef(categories = listOf("cat1", "cat2"))
        viewModel.init(initial)
        viewModel.removeCategory(0)
        assertEquals(listOf("cat2"), viewModel.intentDef.value.categories)
    }

    @Test
    fun `isIntentValid checks for duplicates and empty keys`() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isIntentValid.collect()
        }

        // Valid initial state
        assertTrue(viewModel.isIntentValid.value)

        // Duplicate category
        viewModel.init(IntentDef(categories = listOf("cat", "cat")))
        assertFalse(viewModel.isIntentValid.value)

        // Empty category
        viewModel.init(IntentDef(categories = listOf("")))
        assertFalse(viewModel.isIntentValid.value)

        // Duplicate extra key
        viewModel.init(
            IntentDef(
                extras = listOf(
                    ExtraDef("key", "val1"),
                    ExtraDef("key", "val2"),
                ),
            ),
        )
        assertFalse(viewModel.isIntentValid.value)

        // Empty extra key
        viewModel.init(IntentDef(extras = listOf(ExtraDef("", "val"))))
        assertFalse(viewModel.isIntentValid.value)

        // Invalid extra value for type
        viewModel.init(IntentDef(extras = listOf(ExtraDef("key", "not_int", ExtraType.INT))))
        assertFalse(viewModel.isIntentValid.value)

        // All valid
        viewModel.init(
            IntentDef(
                categories = listOf("cat1", "cat2"),
                extras = listOf(
                    ExtraDef("key1", "val"),
                    ExtraDef("key2", "123", ExtraType.INT),
                ),
            ),
        )
        assertTrue(viewModel.isIntentValid.value)

        job.cancel()
    }
}
