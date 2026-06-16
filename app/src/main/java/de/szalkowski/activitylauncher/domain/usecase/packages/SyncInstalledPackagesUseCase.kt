package de.szalkowski.activitylauncher.domain.usecase.packages

import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import javax.inject.Inject

class SyncInstalledPackagesUseCase @Inject constructor(
    private val packageRepository: PackageRepository,
) {
    suspend operator fun invoke() {
        packageRepository.sync()
    }
}
