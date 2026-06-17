package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreatorProxy
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val context: android.content.Context = mock()
    private val shortcutCreator: ShortcutCreatorProxy = mock()
    private val recentsRepository: RecentsRepository = mock()
    private lateinit var useCase: CreateShortcutUseCase

    private val componentName = ComponentName("com.test", "Activity")
    private val activityInfo = MyActivityInfo(componentName, "Test", null, false)

    @Before
    fun setup() {
        whenever(context.getText(any())).thenReturn("Test")
        useCase = CreateShortcutUseCase(context, shortcutCreator, recentsRepository)
    }

    @Test
    fun `should create shortcut and add to recents`() {
        useCase.invoke(activityInfo, null, showToast = false)

        verify(shortcutCreator).createLauncherIcon(eq(activityInfo), isNull(), isNull())
        verify(recentsRepository).addActivity(componentName)
    }
}
