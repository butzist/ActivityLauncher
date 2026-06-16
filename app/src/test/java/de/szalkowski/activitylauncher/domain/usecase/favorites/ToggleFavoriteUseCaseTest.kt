package de.szalkowski.activitylauncher.domain.usecase.favorites

import android.content.ComponentName
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ToggleFavoriteUseCaseTest {
    private val favoritesRepository: FavoritesRepository = mock()
    private lateinit var useCase: ToggleFavoriteUseCase
    private val componentName = ComponentName("com.test", "Activity")

    @Before
    fun setup() {
        useCase = ToggleFavoriteUseCase(favoritesRepository)
    }

    @Test
    fun `should add to favorites if not already a favorite`() {
        whenever(favoritesRepository.isFavorite(componentName)).thenReturn(false)

        useCase.invoke(componentName)

        verify(favoritesRepository).addFavorite(componentName)
    }

    @Test
    fun `should remove from favorites if already a favorite`() {
        whenever(favoritesRepository.isFavorite(componentName)).thenReturn(true)

        useCase.invoke(componentName)

        verify(favoritesRepository).removeFavorite(componentName)
    }
}
