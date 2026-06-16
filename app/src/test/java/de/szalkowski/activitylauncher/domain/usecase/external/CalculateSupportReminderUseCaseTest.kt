package de.szalkowski.activitylauncher.domain.usecase.external

import de.szalkowski.activitylauncher.domain.external.SupportReminder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CalculateSupportReminderUseCaseTest {
    private val supportReminder: SupportReminder = mock()
    private lateinit var useCase: CalculateSupportReminderUseCase

    @Before
    fun setup() {
        useCase = CalculateSupportReminderUseCase(supportReminder)
    }

    @Test
    fun `should return true when reminder should be displayed`() {
        whenever(supportReminder.shouldDisplayReminder()).thenReturn(true)
        assertTrue(useCase.invoke())
    }

    @Test
    fun `should return false when reminder should not be displayed`() {
        whenever(supportReminder.shouldDisplayReminder()).thenReturn(false)
        assertFalse(useCase.invoke())
    }
}
