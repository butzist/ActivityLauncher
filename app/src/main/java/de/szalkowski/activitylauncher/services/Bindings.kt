package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
abstract class ServicesModule {
    @Binds
    abstract fun bindActivityLauncherService(
        activityLauncherServiceImpl: ActivityLauncherServiceImpl
    ): ActivityLauncherService

    @Binds
    abstract fun bindActivityListService(
        activityListServiceImpl: ActivityListServiceImpl
    ): ActivityListService

    @Binds
    abstract fun bindPackageListService(
        packageListServiceImpl: PackageListServiceImpl
    ): PackageListService

    @Binds
    abstract fun bindIconCreatorService(
        iconCreatorServiceImpl: IconCreatorServiceImpl
    ): IconCreatorService
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationServicesModule {
    @Singleton
    @Binds
    abstract fun bindIntentSigningService(
        intentSigningServiceImpl: IntentSigningServiceImpl
    ): IntentSigningService

    @Singleton
    @Binds
    abstract fun bindRootDetectionService(
        rootDetectionServiceImpl: RootDetectionServiceImpl
    ): RootDetectionService

    @Singleton
    @Binds
    abstract fun bindSettingsService(
        settingsServiceImpl: SettingsServiceImpl
    ): SettingsService
}