package de.szalkowski.activitylauncher.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.szalkowski.activitylauncher.data.packages.SystemPackageRepositoryImpl
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {
    @Singleton
    @Binds
    abstract fun bindSystemPackageRepository(
        systemPackageRepositoryImpl: SystemPackageRepositoryImpl,
    ): SystemPackageRepository
}
