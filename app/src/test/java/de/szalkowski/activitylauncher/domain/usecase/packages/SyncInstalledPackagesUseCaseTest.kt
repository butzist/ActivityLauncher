package de.szalkowski.activitylauncher.domain.usecase.packages

import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class SyncInstalledPackagesUseCaseTest {
    private val packageRepository: PackageRepository = mock()
    private lateinit var useCase: SyncInstalledPackagesUseCase

    @Before
    fun setup() {
        useCase = SyncInstalledPackagesUseCase(packageRepository)
    }

    @Test
    fun `should trigger sync on repository`() = runTest {
        useCase()
        verify(packageRepository).sync()
    }
}
