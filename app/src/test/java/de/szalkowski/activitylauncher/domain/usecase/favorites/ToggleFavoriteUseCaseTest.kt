package de.szalkowski.activitylauncher.domain.usecase.favorites

import android.content.ComponentName
import android.content.Intent
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ToggleFavoriteUseCaseTest {
    private val favoritesRepository: FavoritesRepository = mock()
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setup() {
        useCase = ToggleFavoriteUseCase(favoritesRepository)
    }

    @Test
    fun `invoke adds favorite when not present`() {
        val component = ComponentName("pkg", "cls")
        val intent = mock<Intent>()
        whenever(intent.component).thenReturn(component)
        val request = ShortcutRequest("name", intent, ActivityIcon.Resource("pkg", 1), source = LaunchSource.PRIMARY)
        whenever(favoritesRepository.isFavorite(component)).thenReturn(false)

        val result = useCase(request)

        assertTrue(result)
        verify(favoritesRepository).addFavorite(request)
    }

    @Test
    fun `invoke removes favorite when present`() {
        val component = ComponentName("pkg", "cls")
        val intent = mock<Intent>()
        whenever(intent.component).thenReturn(component)
        val request = ShortcutRequest("name", intent, ActivityIcon.Resource("pkg", 1), source = LaunchSource.PRIMARY)
        whenever(favoritesRepository.isFavorite(component)).thenReturn(true)

        val result = useCase(request)

        assertFalse(result)
        verify(favoritesRepository).removeFavorite(request)
    }
}
