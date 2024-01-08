package de.szalkowski.activitylauncher.services

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
abstract class ServicesModule {
    @ActivityScoped
    @Binds
    abstract fun bindActivityLauncherService(
        activityLauncherServiceImpl: ActivityLauncherServiceImpl
    ): ActivityLauncherService

    @ActivityScoped
    @Binds
    abstract fun bindActivityListService(
        activityListServiceImpl: ActivityListServiceImpl
    ): ActivityListService

    @ActivityScoped
    @Binds
    abstract fun bindPackageListService(
        packageListServiceImpl: PackageListServiceImpl
    ): PackageListService

    @ActivityScoped
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