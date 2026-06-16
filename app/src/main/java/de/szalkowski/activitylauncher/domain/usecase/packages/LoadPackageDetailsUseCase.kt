package de.szalkowski.activitylauncher.domain.usecase.packages

import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import javax.inject.Inject

class LoadPackageDetailsUseCase @Inject constructor(
    private val packageRepository: PackageRepository,
) {
    suspend operator fun invoke() {
        packageRepository.sync()
    }
}
