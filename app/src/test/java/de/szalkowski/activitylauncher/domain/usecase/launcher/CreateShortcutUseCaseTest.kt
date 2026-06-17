package de.szalkowski.activitylauncher.domain.usecase.launcher

import android.content.ComponentName
import android.graphics.drawable.Drawable
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class CreateShortcutUseCaseTest {
    private val shortcutCreator: ShortcutCreator = mock()
    private val recentsRepository: RecentsRepository = mock()
    private val getActivityIconUseCase: GetActivityIconUseCase = mock()
    private val mockDrawable: Drawable = mock()
    private lateinit var useCase: CreateShortcutUseCase

    private val componentName = ComponentName("com.test", "Activity")
    private val activityInfo = MyActivityInfo(componentName, "Test", null, false)

    @Before
    fun setup() {
        whenever(getActivityIconUseCase.invoke(anyOrNull(), any())).thenReturn(mockDrawable)
        useCase = CreateShortcutUseCase(shortcutCreator, recentsRepository, getActivityIconUseCase)
    }

    @Test
    fun `should create shortcut and add to recents`() {
        useCase.invoke(activityInfo, false)

        verify(shortcutCreator).createLauncherIcon(activityInfo, mockDrawable, null)
        verify(recentsRepository).addActivity(componentName, false)
    }

    @Test
    fun `should create root shortcut and add to recents`() {
        useCase.invoke(activityInfo, true)

        verify(shortcutCreator).createRootLauncherIcon(activityInfo, mockDrawable, null)
        verify(recentsRepository).addActivity(componentName, true)
    }
}
