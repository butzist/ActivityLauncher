package de.szalkowski.activitylauncher.domain.usecase.packages

import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class LoadPackageDetailsUseCaseTest {
    private val packageRepository: PackageRepository = mock()
    private lateinit var useCase: LoadPackageDetailsUseCase

    @Before
    fun setup() {
        useCase = LoadPackageDetailsUseCase(packageRepository)
    }

    @Test
    fun `should trigger sync on repository`() = runTest {
        useCase()
        verify(packageRepository).sync()
    }
}
