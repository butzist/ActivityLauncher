package de.szalkowski.activitylauncher.di

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import de.szalkowski.activitylauncher.FakeSystemPackageRepository
import de.szalkowski.activitylauncher.app.di.SystemModule
import de.szalkowski.activitylauncher.domain.packages.SystemPackageRepository
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SystemModule::class],
)
abstract class TestSystemModule {
    @Singleton
    @Binds
    abstract fun bindSystemPackageRepository(
        fakeSystemPackageRepository: FakeSystemPackageRepository,
    ): SystemPackageRepository
}
